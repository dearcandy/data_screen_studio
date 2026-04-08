package com.example.datascreen.service.source;

import com.example.datascreen.model.SourceType;
import com.example.datascreen.service.StudioPaths;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Component
public class ExcelSourceHandler implements DataSourceTypeHandler {

    private static final int DEFAULT_SHEET_INDEX = 0;
    private static final Set<String> ALLOWED_JSON_KEYS = Set.of("sheet", "sheetIndex");
    private final StudioPaths studioPaths;
    private final ObjectMapper objectMapper;

    public ExcelSourceHandler(StudioPaths studioPaths, ObjectMapper objectMapper) {
        this.studioPaths = studioPaths;
        this.objectMapper = objectMapper;
    }

    @Override
    public SourceType supportType() {
        return SourceType.EXCEL;
    }

    @Override
    public void validateConfig(JsonNode cfg) {
        HandlerValidationSupport.checkUnknown(cfg, Set.of("fileId"));
        HandlerValidationSupport.require(cfg, "fileId");
    }

    @Override
    public void testConnection(JsonNode cfg) throws Exception {
        Path path = resolveFilePath(cfg);
        try (InputStream in = Files.newInputStream(path);
             Workbook wb = WorkbookFactory.create(in)) {
            if (wb.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("Excel 文件无工作表");
            }
        }
    }

    @Override
    public Object fetch(JsonNode cfg, String fetchSpec) throws Exception {
        Path path = resolveFilePath(cfg);
        try (InputStream in = Files.newInputStream(path);
             Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = resolveSheet(wb, fetchSpec);
            return readSheetToRows(sheet, wb);
        }
    }

    // ========== 私有辅助方法 ==========

    private Path resolveFilePath(JsonNode cfg) {
        String fileId = cfg.path("fileId").asText("");
        if (fileId.isBlank()) {
            throw new IllegalArgumentException("Excel 配置缺少 fileId（请先上传文件）");
        }
        Path path = studioPaths.resolveUpload(fileId);
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Excel 文件不存在: " + fileId);
        }
        return path;
    }

    private Sheet resolveSheet(Workbook wb, String fetchSpec) {
        if (fetchSpec == null || fetchSpec.isBlank()) {
            return wb.getSheetAt(DEFAULT_SHEET_INDEX);
        }
        String trimmed = fetchSpec.trim();
        // 尝试解析 JSON 格式
        if (trimmed.startsWith("{")) {
            try {
                JsonNode node = objectMapper.readTree(trimmed);
                if (!node.isObject()) {
                    throw new IllegalArgumentException("fetchSpec JSON 必须是对象");
                }
                HandlerValidationSupport.checkUnknown(node, ALLOWED_JSON_KEYS);
                if (node.has("sheet")) {
                    String sheetName = node.path("sheet").asText("");
                    Sheet sheet = wb.getSheet(sheetName);
                    if (sheet == null) {
                        throw new IllegalArgumentException("找不到工作表: " + sheetName);
                    }
                    return sheet;
                }
                if (node.has("sheetIndex")) {
                    int idx = node.path("sheetIndex").asInt(DEFAULT_SHEET_INDEX);
                    if (idx < 0 || idx >= wb.getNumberOfSheets()) {
                        throw new IllegalArgumentException("sheetIndex 超出范围: " + idx);
                    }
                    return wb.getSheetAt(idx);
                }
                throw new IllegalArgumentException("JSON fetchSpec 需包含 sheet 或 sheetIndex");
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException("fetchSpec JSON 解析失败: " + e.getMessage());
            }
        }
        // 兼容原始用法：直接传 sheet 名称
        Sheet sheet = wb.getSheet(trimmed);
        if (sheet == null) {
            throw new IllegalArgumentException("找不到工作表: " + trimmed);
        }
        return sheet;
    }

    private List<Map<String, Object>> readSheetToRows(Sheet sheet, Workbook wb) {
        Row headerRow = sheet.getRow(sheet.getFirstRowNum());
        if (headerRow == null) {
            return Collections.emptyList();
        }

        // 读取表头
        List<String> columnNames = new ArrayList<>();
        for (int c = headerRow.getFirstCellNum(); c < headerRow.getLastCellNum(); c++) {
            Cell cell = headerRow.getCell(c);
            String name = (cell != null) ? cell.toString().trim() : "col" + c;
            if (name.isEmpty()) {
                name = "col" + c;
            }
            columnNames.add(name);
        }

        // 创建公式求值器（用于公式单元格）
        FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();

        List<Map<String, Object>> rows = new ArrayList<>();
        int firstDataRow = sheet.getFirstRowNum() + 1;
        int lastRowNum = sheet.getLastRowNum();
        for (int r = firstDataRow; r <= lastRowNum; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            // 跳过完全空行
            if (isEmptyRow(row, columnNames.size())) {
                continue;
            }
            Map<String, Object> rowMap = new LinkedHashMap<>(columnNames.size());
            for (int c = 0; c < columnNames.size(); c++) {
                Cell cell = row.getCell(c);
                rowMap.put(columnNames.get(c), getCellValue(cell, evaluator));
            }
            rows.add(rowMap);
        }
        return rows;
    }

    private boolean isEmptyRow(Row row, int columnCount) {
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum() && i < columnCount; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    private Object getCellValue(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return null;
        }
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            // 计算公式值
            CellValue evaluated = evaluator.evaluate(cell);
            if (evaluated == null) {
                return null;
            }
            return switch (evaluated.getCellType()) {
                case NUMERIC -> evaluated.getNumberValue();
                case STRING -> evaluated.getStringValue();
                case BOOLEAN -> evaluated.getBooleanValue();
                default -> null;
            };
        }
        return switch (type) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toString(); // 可改为返回 LocalDateTime
                }
                yield cell.getNumericCellValue();
            }
            case BOOLEAN -> cell.getBooleanCellValue();
            default -> null;
        };
    }
}
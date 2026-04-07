package com.example.datascreen.service.source;

import com.example.datascreen.model.SourceType;
import com.example.datascreen.service.StudioPaths;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ExcelSourceHandler implements DataSourceTypeHandler {
    private final StudioPaths studioPaths;

    public ExcelSourceHandler(StudioPaths studioPaths) {
        this.studioPaths = studioPaths;
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
        String fileId = cfg.path("fileId").asText("");
        if (fileId.isBlank()) {
            throw new IllegalArgumentException("Excel 配置缺少 fileId（请先上传文件）");
        }
        Path path = studioPaths.resolveUpload(fileId);
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Excel 文件不存在: " + fileId);
        }
        try (InputStream in = Files.newInputStream(path);
             Workbook wb = WorkbookFactory.create(in)) {
            if (wb.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("Excel 无工作表");
            }
        }
    }

    @Override
    public Object fetch(JsonNode cfg, String fetchSpec) throws Exception {
        String fileId = cfg.path("fileId").asText("");
        if (fileId.isBlank()) {
            throw new IllegalArgumentException("Excel 配置缺少 fileId");
        }
        Path path = studioPaths.resolveUpload(fileId);
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Excel 文件不存在: " + fileId);
        }
        try (InputStream in = Files.newInputStream(path);
             Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet;
            if (fetchSpec != null && !fetchSpec.isBlank()) {
                sheet = wb.getSheet(fetchSpec);
                if (sheet == null) {
                    throw new IllegalArgumentException("找不到工作表: " + fetchSpec);
                }
            } else {
                sheet = wb.getSheetAt(0);
            }
            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) {
                return new ArrayList<>();
            }
            List<String> names = new ArrayList<>();
            for (int c = header.getFirstCellNum(); c < header.getLastCellNum(); c++) {
                Cell cell = header.getCell(c);
                names.add(cell != null ? cell.toString().trim() : "col" + c);
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                Map<String, Object> m = new LinkedHashMap<>();
                for (int c = 0; c < names.size(); c++) {
                    m.put(names.get(c), cellValue(row.getCell(c)));
                }
                rows.add(m);
            }
            return rows;
        }
    }

    private static Object cellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toString()
                    : cell.getNumericCellValue();
            case BOOLEAN -> cell.getBooleanCellValue();
            case FORMULA -> cell.getCellFormula();
            default -> null;
        };
    }
}

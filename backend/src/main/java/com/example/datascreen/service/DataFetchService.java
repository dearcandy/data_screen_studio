package com.example.datascreen.service;

import com.example.datascreen.entity.DataSetEntity;
import com.example.datascreen.entity.DataSourceEntity;
import com.example.datascreen.model.FetchMode;
import com.example.datascreen.model.SourceType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据拉取服务：根据数据源类型把原始数据标准化为 Java 对象。
 */
@Service
public class DataFetchService {

    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final StudioPaths studioPaths;

    public DataFetchService(ObjectMapper objectMapper, StudioPaths studioPaths) {
        this.objectMapper = objectMapper;
        this.studioPaths = studioPaths;
        this.webClient = WebClient.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(32 * 1024 * 1024))
                .build();
    }

    /** 按数据集配置执行原始取数，不做脚本处理。 */
    public Object fetchRaw(DataSourceEntity source, DataSetEntity dataSet) throws Exception {
        if (dataSet.getFetchMode() == FetchMode.MOCK) {
            if (dataSet.getMockJson() == null || dataSet.getMockJson().isBlank()) {
                return objectMapper.createArrayNode();
            }
            return objectMapper.readValue(dataSet.getMockJson(), Object.class);
        }
        if (source == null) {
            throw new IllegalArgumentException("LIVE 模式需要绑定数据源");
        }
        JsonNode cfg = objectMapper.readTree(source.getConfigJson());
        String spec = dataSet.getFetchSpec() != null ? dataSet.getFetchSpec().trim() : "";
        return switch (source.getType()) {
            case MYSQL, POSTGRESQL -> fetchJdbc(source.getType(), cfg, spec);
            case HTTP_API -> fetchHttp(cfg, spec);
            case REDIS -> fetchRedis(cfg, spec);
            case EXCEL -> fetchExcel(cfg, spec);
            case MOCK -> fetchMockFromSource(cfg);
        };
    }

    /** 从 Mock 数据源配置中读取 mock 字段。 */
    private Object fetchMockFromSource(JsonNode cfg) throws Exception {
        if (cfg.has("mock")) {
            return objectMapper.treeToValue(cfg.get("mock"), Object.class);
        }
        return objectMapper.readValue("[]", Object.class);
    }

    /** JDBC 查询，仅允许 SELECT/WITH。 */
    private Object fetchJdbc(SourceType type, JsonNode cfg, String sql) throws Exception {
        if (sql.isEmpty()) {
            throw new IllegalArgumentException("请填写 SQL（数据集 fetchSpec）");
        }
        String normalized = sql.stripLeading();
        if (!normalized.regionMatches(true, 0, "SELECT", 0, 6)
                && !normalized.regionMatches(true, 0, "WITH", 0, 4)) {
            throw new IllegalArgumentException("仅允许 SELECT / WITH 查询");
        }
        String jdbcUrl = type == SourceType.MYSQL
                ? ConnectionTestService.buildMysqlUrl(cfg)
                : ConnectionTestService.buildPostgresUrl(cfg);
        String user = cfg.path("username").asText("");
        String pass = cfg.path("password").asText("");
        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, pass);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return resultSetToList(rs);
        }
    }

    /** 将 ResultSet 转成列表结构，便于 JSON 序列化。 */
    private List<Map<String, Object>> resultSetToList(ResultSet rs) throws Exception {
        ResultSetMetaData md = rs.getMetaData();
        int cols = md.getColumnCount();
        List<Map<String, Object>> rows = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= cols; i++) {
                String label = md.getColumnLabel(i);
                row.put(label, rs.getObject(i));
            }
            rows.add(row);
        }
        return rows;
    }

    /** 通过 HTTP GET 取数。 */
    private Object fetchHttp(JsonNode cfg, String pathOrEmpty) throws Exception {
        String base = cfg.path("baseUrl").asText("");
        if (base.isBlank()) {
            throw new IllegalArgumentException("HTTP 配置缺少 baseUrl");
        }
        String path = pathOrEmpty.isBlank() ? "/" : pathOrEmpty;
        String url = joinUrl(base, path);
        HttpHeaders headers = new HttpHeaders();
        if (cfg.has("headers") && cfg.get("headers").isObject()) {
            cfg.get("headers").fields().forEachRemaining(e -> headers.add(e.getKey(), e.getValue().asText()));
        }
        String body = webClient.get()
                .uri(URI.create(url))
                .headers(h -> h.addAll(headers))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .block();
        if (body == null || body.isBlank()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readValue(body, Object.class);
    }

    /** 按 key 读取 Redis 值，优先按 JSON 解析。 */
    private Object fetchRedis(JsonNode cfg, String key) {
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Redis 请在数据集 fetchSpec 中填写 key");
        }
        String host = cfg.path("host").asText("127.0.0.1");
        int port = cfg.path("port").asInt(6379);
        String password = cfg.path("password").asText(null);
        int db = cfg.path("database").asInt(0);
        RedisURI.Builder b = RedisURI.builder().withHost(host).withPort(port).withDatabase(db).withTimeout(Duration.ofSeconds(5));
        if (password != null && !password.isEmpty()) {
            b.withPassword(password.toCharArray());
        }
        RedisClient client = RedisClient.create(b.build());
        try (StatefulRedisConnection<String, String> conn = client.connect()) {
            String val = conn.sync().get(key);
            if (val == null) {
                return objectMapper.createObjectNode();
            }
            try {
                return objectMapper.readValue(val, Object.class);
            } catch (Exception e) {
                return val;
            }
        } finally {
            client.shutdown();
        }
    }

    /** 读取 Excel 并按首行表头映射为对象数组。 */
    private Object fetchExcel(JsonNode cfg, String sheetNameOrEmpty) throws Exception {
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
            if (sheetNameOrEmpty != null && !sheetNameOrEmpty.isBlank()) {
                sheet = wb.getSheet(sheetNameOrEmpty);
                if (sheet == null) {
                    throw new IllegalArgumentException("找不到工作表: " + sheetNameOrEmpty);
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
                    Cell cell = row.getCell(c);
                    m.put(names.get(c), cellValue(cell));
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

    private static String joinUrl(String base, String path) {
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        if (!base.endsWith("/") && !path.startsWith("/")) {
            return base + "/" + path;
        }
        if (base.endsWith("/") && path.startsWith("/")) {
            return base.substring(0, base.length() - 1) + path;
        }
        return base + path;
    }
}

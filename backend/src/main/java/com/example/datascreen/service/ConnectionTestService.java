package com.example.datascreen.service;

import com.example.datascreen.model.SourceType;
import com.fasterxml.jackson.databind.JsonNode;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
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
import java.time.Duration;
import java.util.Locale;

/**
 * 数据源连通性测试服务。
 */
@Service
public class ConnectionTestService {

    private final DataSourceConfigValidator configValidator;
    private final WebClient webClient;
    private final StudioPaths studioPaths;

    public ConnectionTestService(DataSourceConfigValidator configValidator, StudioPaths studioPaths) {
        this.configValidator = configValidator;
        this.studioPaths = studioPaths;
        this.webClient = WebClient.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }

    /** 按数据源类型分发对应测试逻辑。 */
    public void test(SourceType type, String configJson) throws Exception {
        JsonNode cfg = configValidator.validate(type, configJson);
        switch (type) {
            case MYSQL -> testJdbc(buildMysqlUrl(cfg), cfg.path("username").asText(""), cfg.path("password").asText(""));
            case POSTGRESQL -> testJdbc(buildPostgresUrl(cfg), cfg.path("username").asText(""), cfg.path("password").asText(""));
            case HTTP_API -> testHttp(cfg);
            case REDIS -> testRedis(cfg);
            case EXCEL -> testExcel(cfg);
            case MOCK -> { /* always ok */ }
            default -> throw new IllegalArgumentException("未知数据源类型: " + type);
        }
    }

    /** JDBC 可用性测试。 */
    private void testJdbc(String jdbcUrl, String user, String pass) throws Exception {
        try (Connection c = DriverManager.getConnection(jdbcUrl, user, pass)) {
            c.isValid(3);
        }
    }

    /** HTTP API 连通性测试（GET）。 */
    private void testHttp(JsonNode cfg) {
        String base = cfg.path("baseUrl").asText("");
        if (base.isBlank()) {
            throw new IllegalArgumentException("HTTP 配置缺少 baseUrl");
        }
        String path = cfg.path("testPath").asText("/");
        String url = joinUrl(base, path);
        HttpHeaders headers = new HttpHeaders();
        if (cfg.has("headers") && cfg.get("headers").isObject()) {
            cfg.get("headers").fields().forEachRemaining(e -> headers.add(e.getKey(), e.getValue().asText()));
        }
        webClient.get()
                .uri(URI.create(url))
                .headers(h -> h.addAll(headers))
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(15))
                .block();
    }

    /** Redis 连通性测试（PING）。 */
    private void testRedis(JsonNode cfg) {
        String host = cfg.path("host").asText("127.0.0.1");
        int port = cfg.path("port").asInt(6379);
        String password = cfg.path("password").asText(null);
        int db = cfg.path("database").asInt(0);
        RedisURI.Builder b = RedisURI.builder().withHost(host).withPort(port).withDatabase(db).withTimeout(Duration.ofSeconds(3));
        if (password != null && !password.isEmpty()) {
            b.withPassword(password.toCharArray());
        }
        RedisClient client = RedisClient.create(b.build());
        try (StatefulRedisConnection<String, String> conn = client.connect()) {
            conn.sync().ping();
        } finally {
            client.shutdown();
        }
    }

    /** Excel 文件合法性测试（存在且至少有一个工作表）。 */
    private void testExcel(JsonNode cfg) throws Exception {
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

    /** 根据配置构建 MySQL JDBC URL。 */
    public static String buildMysqlUrl(JsonNode cfg) {
        String host = cfg.path("host").asText("127.0.0.1");
        int port = cfg.path("port").asInt(3306);
        String db = cfg.path("database").asText("test");
        return String.format(Locale.ROOT, "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&characterEncoding=utf8",
                host, port, db);
    }

    /** 根据配置构建 PostgreSQL JDBC URL。 */
    public static String buildPostgresUrl(JsonNode cfg) {
        String host = cfg.path("host").asText("127.0.0.1");
        int port = cfg.path("port").asInt(5432);
        String db = cfg.path("database").asText("postgres");
        return String.format(Locale.ROOT, "jdbc:postgresql://%s:%d/%s", host, port, db);
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

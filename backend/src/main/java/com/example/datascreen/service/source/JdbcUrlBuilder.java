package com.example.datascreen.service.source;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Locale;
import java.util.StringJoiner;

/**
 * JDBC URL 构建工具（支持 MySQL / PGSQL）
 */
final class JdbcUrlBuilder {

    // MySQL 默认值
    private static final String MYSQL_DEFAULT_HOST = "127.0.0.1";
    private static final int MYSQL_DEFAULT_PORT = 3306;
    private static final String MYSQL_DEFAULT_DB = "test";
    private static final String MYSQL_URL_PREFIX = "jdbc:mysql://%s:%d/%s";

    // PGSQL 默认值
    private static final String PG_DEFAULT_HOST = "127.0.0.1";
    private static final int PG_DEFAULT_PORT = 5432;
    private static final String PG_DEFAULT_DB = "postgres";
    private static final String PG_URL_PREFIX = "jdbc:postgresql://%s:%d/%s";

    private JdbcUrlBuilder() {
        // 禁止实例化
    }

    /**
     * 构建 MySQL JDBC URL，支持自定义连接参数
     *
     * @param cfg 配置节点，至少应包含 host、port、database（均有默认值）
     * @return JDBC URL
     * @throws IllegalArgumentException 当端口号超出有效范围时抛出
     */
    static String buildMysqlUrl(JsonNode cfg) {
        String host = getHost(cfg, MYSQL_DEFAULT_HOST);
        int port = getPort(cfg, MYSQL_DEFAULT_PORT);
        String database = getDatabase(cfg, MYSQL_DEFAULT_DB);

        String baseUrl = String.format(Locale.ROOT, MYSQL_URL_PREFIX, host, port, database);
        String params = buildMysqlParams(cfg);
        return baseUrl + "?" + params;
    }

    /**
     * 构建 PGSQL JDBC URL
     *
     * @param cfg 配置节点，至少应包含 host、port、database（均有默认值）
     * @return JDBC URL
     * @throws IllegalArgumentException 当端口号超出有效范围时抛出
     */
    static String buildPostgresUrl(JsonNode cfg) {
        String host = getHost(cfg, PG_DEFAULT_HOST);
        int port = getPort(cfg, PG_DEFAULT_PORT);
        String database = getDatabase(cfg, PG_DEFAULT_DB);

        return String.format(Locale.ROOT, PG_URL_PREFIX, host, port, database);
    }

    // ========== 私有辅助方法 ==========

    private static String getHost(JsonNode cfg, String defaultHost) {
        if (cfg == null) {
            return defaultHost;
        }
        String host = cfg.path("host").asText(defaultHost).trim();
        return host.isEmpty() ? defaultHost : host;
    }

    private static int getPort(JsonNode cfg, int defaultPort) {
        if (cfg == null) {
            return defaultPort;
        }
        int port = cfg.path("port").asInt(defaultPort);
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("端口号必须在 1~65535 之间，当前值: " + port);
        }
        return port;
    }

    private static String getDatabase(JsonNode cfg, String defaultDb) {
        if (cfg == null) {
            return defaultDb;
        }
        String db = cfg.path("database").asText(defaultDb).trim();
        return db.isEmpty() ? defaultDb : db;
    }

    /**
     * 构建 MySQL 连接参数，支持从 cfg 中读取额外参数并覆盖默认值
     */
    private static String buildMysqlParams(JsonNode cfg) {
        // 默认参数
        boolean useSSL = false;
        String serverTimezone = "UTC";
        String encoding = "utf8";

        // 允许从 cfg 中覆盖
        if (cfg != null) {
            if (cfg.has("useSSL")) {
                useSSL = cfg.path("useSSL").asBoolean(false);
            }
            if (cfg.has("serverTimezone")) {
                serverTimezone = cfg.path("serverTimezone").asText("UTC").trim();
            }
            if (cfg.has("characterEncoding")) {
                encoding = cfg.path("characterEncoding").asText("utf8").trim();
            }
        }

        StringJoiner params = new StringJoiner("&");
        params.add("useSSL=" + useSSL);
        params.add("serverTimezone=" + serverTimezone);
        params.add("characterEncoding=" + encoding);
        return params.toString();
    }
}
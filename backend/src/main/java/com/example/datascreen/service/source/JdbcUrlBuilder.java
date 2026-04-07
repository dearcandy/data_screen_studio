package com.example.datascreen.service.source;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Locale;

final class JdbcUrlBuilder {
    private JdbcUrlBuilder() {
    }

    static String buildMysqlUrl(JsonNode cfg) {
        String host = cfg.path("host").asText("127.0.0.1");
        int port = cfg.path("port").asInt(3306);
        String db = cfg.path("database").asText("test");
        return String.format(Locale.ROOT, "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&characterEncoding=utf8", host, port, db);
    }

    static String buildPostgresUrl(JsonNode cfg) {
        String host = cfg.path("host").asText("127.0.0.1");
        int port = cfg.path("port").asInt(5432);
        String db = cfg.path("database").asText("postgres");
        return String.format(Locale.ROOT, "jdbc:postgresql://%s:%d/%s", host, port, db);
    }
}

package com.example.datascreen.service;

import com.example.datascreen.model.SourceType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * 数据源配置校验器：按 SourceType 强约束 configJson 的必填结构。
 */
@Component
public class DataSourceConfigValidator {

    private final ObjectMapper objectMapper;

    public DataSourceConfigValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode validate(SourceType type, String configJson) {
        if (configJson == null || configJson.isBlank()) {
            throw new IllegalArgumentException("配置 JSON 不能为空");
        }
        try {
            JsonNode cfg = objectMapper.readTree(configJson);
            if (!cfg.isObject()) {
                throw new IllegalArgumentException("配置必须是 JSON 对象");
            }
            return validateByType(type, cfg);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("配置 JSON 格式错误: " + e.getMessage());
        }
    }

    private JsonNode validateByType(SourceType type, JsonNode cfg) {
        return switch (type) {
            case MYSQL -> {
                checkUnknown(cfg, Set.of("host", "port", "database", "username", "password"));
                require(cfg, "host", "port", "database", "username", "password");
                requirePositiveInt(cfg, "port");
                yield cfg;
            }
            case POSTGRESQL -> {
                checkUnknown(cfg, Set.of("host", "port", "database", "username", "password"));
                require(cfg, "host", "port", "database", "username", "password");
                requirePositiveInt(cfg, "port");
                yield cfg;
            }
            case HTTP_API -> {
                checkUnknown(cfg, Set.of("baseUrl", "testPath", "headers"));
                require(cfg, "baseUrl");
                if (cfg.has("headers") && !cfg.get("headers").isObject()) {
                    throw new IllegalArgumentException("headers 必须是 JSON 对象");
                }
                yield cfg;
            }
            case REDIS -> {
                checkUnknown(cfg, Set.of("host", "port", "password", "database"));
                require(cfg, "host", "port");
                requirePositiveInt(cfg, "port");
                if (cfg.has("database")) {
                    requireNonNegativeInt(cfg, "database");
                }
                yield cfg;
            }
            case EXCEL -> {
                checkUnknown(cfg, Set.of("fileId"));
                require(cfg, "fileId");
                yield cfg;
            }
            case MOCK -> {
                checkUnknown(cfg, Set.of("mock"));
                if (!cfg.has("mock")) {
                    throw new IllegalArgumentException("MOCK 配置缺少字段: mock");
                }
                yield cfg;
            }
        };
    }

    private JsonNode require(JsonNode cfg, String... keys) {
        for (String k : keys) {
            JsonNode v = cfg.get(k);
            if (v == null || v.isNull() || (v.isTextual() && v.asText().isBlank())) {
                throw new IllegalArgumentException("配置缺少或为空字段: " + k);
            }
        }
        return cfg;
    }

    private void requirePositiveInt(JsonNode cfg, String key) {
        JsonNode v = cfg.get(key);
        if (v == null || !v.canConvertToInt() || v.asInt() <= 0) {
            throw new IllegalArgumentException(key + " 必须是正整数");
        }
    }

    private void requireNonNegativeInt(JsonNode cfg, String key) {
        JsonNode v = cfg.get(key);
        if (v == null || !v.canConvertToInt() || v.asInt() < 0) {
            throw new IllegalArgumentException(key + " 必须是大于等于 0 的整数");
        }
    }

    private void checkUnknown(JsonNode cfg, Set<String> allowedKeys) {
        Set<String> unknown = new HashSet<>();
        Iterator<String> it = cfg.fieldNames();
        while (it.hasNext()) {
            String key = it.next();
            if (!allowedKeys.contains(key)) {
                unknown.add(key);
            }
        }
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("存在未知字段: " + String.join(", ", unknown));
        }
    }
}

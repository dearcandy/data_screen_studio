package com.example.datascreen.service.source;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * 配置校验工具
 */
final class HandlerValidationSupport {
    private HandlerValidationSupport() {
    }

    static void require(JsonNode cfg, String... keys) {
        for (String k : keys) {
            JsonNode v = cfg.get(k);
            if (v == null || v.isNull() || (v.isTextual() && v.asText().isBlank())) {
                throw new IllegalArgumentException("配置缺少或为空字段: " + k);
            }
        }
    }

    static void requirePositiveInt(JsonNode cfg) {
        JsonNode v = cfg.get("port");
        if (v == null || !v.canConvertToInt() || v.asInt() <= 0) {
            throw new IllegalArgumentException("port" + " 必须是正整数");
        }
    }

    static void requireNonNegativeInt(JsonNode cfg) {
        JsonNode v = cfg.get("database");
        if (v == null || !v.canConvertToInt() || v.asInt() < 0) {
            throw new IllegalArgumentException("database" + " 必须是大于等于 0 的整数");
        }
    }

    static void checkUnknown(JsonNode cfg, Set<String> allowedKeys) {
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

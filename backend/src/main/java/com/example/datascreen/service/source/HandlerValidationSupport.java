package com.example.datascreen.service.source;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * 数据源配置校验工具类（线程安全，无状态）
 */
final class HandlerValidationSupport {

    private HandlerValidationSupport() {
        // 禁止实例化
    }

    /**
     * 必填字段校验（字段不能为 null、不能为缺失、字符串不能为空）
     *
     * @param cfg  配置节点
     * @param keys 必填字段名
     */
    static void require(JsonNode cfg, String... keys) {
        if (cfg == null) {
            throw new IllegalArgumentException("配置节点不能为 null");
        }
        for (String k : keys) {
            JsonNode v = cfg.get(k);
            if (v == null || v.isNull() || (v.isTextual() && v.asText().isBlank())) {
                throw new IllegalArgumentException("配置缺少或为空字段: " + k);
            }
        }
    }

    /**
     * 正整数校验（默认字段名为 "port"）
     */
    static void requirePositiveInt(JsonNode cfg) {
        requirePositiveInt(cfg, "port");
    }

    /**
     * 正整数校验（指定字段名）
     */
    static void requirePositiveInt(JsonNode cfg, String key) {
        if (cfg == null) {
            throw new IllegalArgumentException("配置节点不能为 null");
        }
        JsonNode v = cfg.get(key);
        if (v == null || !v.canConvertToInt() || v.asInt() <= 0) {
            throw new IllegalArgumentException(key + " 必须是正整数");
        }
    }

    /**
     * 非负整数校验（默认字段名为 "database"）
     */
    static void requireNonNegativeInt(JsonNode cfg) {
        requireNonNegativeInt(cfg, "database");
    }

    /**
     * 非负整数校验（指定字段名）
     */
    static void requireNonNegativeInt(JsonNode cfg, String key) {
        if (cfg == null) {
            throw new IllegalArgumentException("配置节点不能为 null");
        }
        JsonNode v = cfg.get(key);
        if (v == null || !v.canConvertToInt() || v.asInt() < 0) {
            throw new IllegalArgumentException(key + " 必须是大于等于 0 的整数");
        }
    }

    /**
     * 未知字段校验（防止配置中出现不被允许的额外字段）
     *
     * @param cfg         配置节点
     * @param allowedKeys 允许出现的字段名集合
     */
    static void checkUnknown(JsonNode cfg, Set<String> allowedKeys) {
        if (cfg == null) {
            return; // 空配置无需校验未知字段
        }
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
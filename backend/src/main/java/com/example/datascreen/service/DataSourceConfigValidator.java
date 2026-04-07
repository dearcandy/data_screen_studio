package com.example.datascreen.service;

import com.example.datascreen.model.SourceType;
import com.example.datascreen.service.source.DataSourceHandlerManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * 数据源配置校验器：按 SourceType 强约束 configJson 的必填结构。
 */
@Component
public class DataSourceConfigValidator {

    private final ObjectMapper objectMapper;
    private final DataSourceHandlerManager handlerManager;

    public DataSourceConfigValidator(ObjectMapper objectMapper, DataSourceHandlerManager handlerManager) {
        this.objectMapper = objectMapper;
        this.handlerManager = handlerManager;
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
            handlerManager.get(type).validateConfig(cfg);
            return cfg;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("配置 JSON 格式错误: " + e.getMessage());
        }
    }
}

package com.example.datascreen.service.source;

import com.example.datascreen.model.SourceType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class MockSourceHandler implements DataSourceTypeHandler {
    private final ObjectMapper objectMapper;

    public MockSourceHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public SourceType supportType() {
        return SourceType.MOCK;
    }

    @Override
    public void validateConfig(JsonNode cfg) {
        HandlerValidationSupport.checkUnknown(cfg, Set.of("mock"));
        if (!cfg.has("mock")) {
            throw new IllegalArgumentException("MOCK 配置缺少字段: mock");
        }
    }

    @Override
    public void testConnection(JsonNode cfg) {
        // always ok
    }

    @Override
    public Object fetch(JsonNode cfg, String fetchSpec) throws Exception {
        if (cfg.has("mock")) {
            return objectMapper.treeToValue(cfg.get("mock"), Object.class);
        }
        return objectMapper.readValue("[]", Object.class);
    }
}

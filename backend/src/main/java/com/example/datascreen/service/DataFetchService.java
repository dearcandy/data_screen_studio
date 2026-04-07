package com.example.datascreen.service;

import com.example.datascreen.entity.DataSetEntity;
import com.example.datascreen.entity.DataSourceEntity;
import com.example.datascreen.model.FetchMode;
import com.example.datascreen.service.source.DataSourceHandlerManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

/**
 * 数据拉取服务：根据数据源类型把原始数据标准化为 Java 对象。
 */
@Service
public class DataFetchService {

    private final ObjectMapper objectMapper;
    private final DataSourceHandlerManager handlerManager;

    public DataFetchService(ObjectMapper objectMapper, DataSourceHandlerManager handlerManager) {
        this.objectMapper = objectMapper;
        this.handlerManager = handlerManager;
    }

    /**
     * 按数据集配置执行原始取数，不做脚本处理
     * @param source 数据源
     * @param dataSet 数据集
     * @return 数据对象
     * @throws Exception 对象处理异常
     */
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
        return handlerManager.get(source.getType()).fetch(cfg, spec);
    }
}

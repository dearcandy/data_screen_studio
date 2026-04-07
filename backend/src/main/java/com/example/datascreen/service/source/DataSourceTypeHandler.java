package com.example.datascreen.service.source;

import com.example.datascreen.model.SourceType;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * 按数据源类型处理连通性测试与取数逻辑。
 */
public interface DataSourceTypeHandler {

    /**
     * 数据源类型
     * @return 数据源类型
     */
    SourceType supportType();

    /**
     * 校验该类型的数据源配置。
     */
    default void validateConfig(JsonNode cfg) {
    }

    /**
     * 测试连接
     * @param cfg 连接配置
     */
    void testConnection(JsonNode cfg) throws Exception;

    /**
     * 拉取数据
     * @param cfg 连接配置
     * @param fetchSpec 拉取设置
     * @return 数据
     */
    Object fetch(JsonNode cfg, String fetchSpec) throws Exception;
}

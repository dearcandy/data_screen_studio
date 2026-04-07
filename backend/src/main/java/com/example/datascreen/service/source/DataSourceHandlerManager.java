package com.example.datascreen.service.source;

import com.example.datascreen.model.SourceType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 统一管理 SourceType 与处理器的映射。
 */
@Component
public class DataSourceHandlerManager {

    private final Map<SourceType, DataSourceTypeHandler> handlers;

    public DataSourceHandlerManager(List<DataSourceTypeHandler> handlerList) {
        this.handlers = new EnumMap<>(SourceType.class);
        for (DataSourceTypeHandler handler : handlerList) {
            SourceType type = handler.supportType();
            if (handlers.containsKey(type)) {
                throw new IllegalStateException("重复的数据源处理器: " + type);
            }
            handlers.put(type, handler);
        }
    }

    public DataSourceTypeHandler get(SourceType type) {
        DataSourceTypeHandler handler = handlers.get(type);
        if (handler == null) {
            throw new IllegalArgumentException("未找到数据源处理器: " + type);
        }
        return handler;
    }
}

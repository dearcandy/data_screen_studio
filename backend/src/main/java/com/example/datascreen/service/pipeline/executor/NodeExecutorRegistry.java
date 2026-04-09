package com.example.datascreen.service.pipeline.executor;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NodeExecutorRegistry {

    private final Map<String, NodeExecutor> executorMap = new ConcurrentHashMap<>();
    private final List<NodeExecutor> executors;

    public NodeExecutorRegistry(List<NodeExecutor> executors) {
        this.executors = executors;
    }

    @PostConstruct
    public void init() {
        for (NodeExecutor executor : executors) {
            // 简单方式：supports 方法可判断，但一般每个 executor 只支持一种类型
            // 更直接：通过自定义注解或约定
            // 这里简化，使用类名或固定映射
        }
    }

    public NodeExecutor getExecutor(String nodeType) {
        for (NodeExecutor executor : executors) {
            if (executor.supports(nodeType)) {
                return executor;
            }
        }
        throw new IllegalArgumentException("未找到节点类型对应的执行器: " + nodeType);
    }
}
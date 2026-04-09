package com.example.datascreen.service.pipeline.executor;

import com.example.datascreen.model.NodeType;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 节点执行器注册表：按节点类型索引。
 */
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
            NodeType nodeType = executor.type();
            if (nodeType == null) {
                throw new IllegalStateException("执行器未声明类型: " + executor.getClass().getName());
            }
            String code = nodeType.getCode();
            NodeExecutor prev = executorMap.putIfAbsent(code, executor);
            if (prev != null && prev != executor) {
                throw new IllegalStateException("节点类型重复注册: " + code);
            }
        }
    }

    /**
     * 根据节点类型获取执行器
     * @param nodeType 节点类型
     * @return 对应的执行器
     */
    public NodeExecutor getExecutor(String nodeType) {
        if (nodeType == null || nodeType.isBlank()) {
            throw new IllegalArgumentException("节点类型不能为空");
        }
        String key = nodeType.trim();
        NodeExecutor hit = executorMap.get(key);
        if (hit != null) {
            return hit;
        }
        // 兼容枚举名等写法（内部仍归一化为 code）
        try {
            NodeType nt = NodeType.fromCode(key);
            hit = executorMap.get(nt.getCode());
            if (hit != null) {
                return hit;
            }
        } catch (IllegalArgumentException ignored) {
            // 继续抛未找到
        }
        throw new IllegalArgumentException("未找到节点类型对应的执行器: " + nodeType);
    }
}

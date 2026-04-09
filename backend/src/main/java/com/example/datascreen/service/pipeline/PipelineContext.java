package com.example.datascreen.service.pipeline;

import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PipelineContext {

    private final Map<String, Object> nodeOutputs = new ConcurrentHashMap<>();
    @Getter
    private final Map<String, Object> globalVariables = new ConcurrentHashMap<>();
    
    // 获取指定节点的输出
    public Object getNodeOutput(String nodeId) {
        return nodeOutputs.get(nodeId);
    }
    
    // 设置当前节点的输出
    public void setNodeOutput(String nodeId, Object output) {
        nodeOutputs.put(nodeId, output);
    }
    
    // 获取所有依赖节点的输出（列表形式）
    public List<Object> getDependencyOutputs(List<String> dependsOn) {
        return dependsOn.stream().map(this::getNodeOutput)
                .collect(Collectors.toList());
    }

    public void setGlobalVariables(Map<String, Object> globalVariables) {
        this.globalVariables.clear();
        if (globalVariables != null) {
            this.globalVariables.putAll(globalVariables);
        }
    }
}
package com.example.datascreen.service.pipeline;

import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 管道上下文：存储管道执行时的全局变量和节点输出。
 */
public class PipelineContext {

    /**
     * 节点输出存储：键为节点 ID，值为节点输出。
     */
    private final Map<String, Object> nodeOutputs = new ConcurrentHashMap<>();
    /**
     * 全局变量存储：键为变量名，值为变量值。
     */
    @Getter
    private final Map<String, Object> globalVariables = new ConcurrentHashMap<>();
    
    /**
     * 获取指定节点的输出
     * @param nodeId 节点 ID
     * @return 节点输出
     */
    public Object getNodeOutput(String nodeId) {
        return nodeOutputs.get(nodeId);
    }
    
    /**
     * 设置当前节点的输出
     * @param nodeId 节点 ID
     * @param output 节点输出
     */
    public void setNodeOutput(String nodeId, Object output) {
        nodeOutputs.put(nodeId, output);
    }
    
    /**
     * 获取所有依赖节点的输出（列表形式）
     * @param dependsOn 依赖节点 ID 列表
     * @return 依赖节点输出列表
     */
    public List<Object> getDependencyOutputs(List<String> dependsOn) {
        return dependsOn.stream().map(this::getNodeOutput)
                .collect(Collectors.toList());
    }

    /**
     * 设置全局变量
     * @param globalVariables 全局变量映射
     */
    public void setGlobalVariables(Map<String, Object> globalVariables) {
        this.globalVariables.clear();
        if (globalVariables != null) {
            this.globalVariables.putAll(globalVariables);
        }
    }
}
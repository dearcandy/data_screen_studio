package com.example.datascreen.service.pipeline.executor;

import com.example.datascreen.model.Node;
import com.example.datascreen.service.pipeline.PipelineContext;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class AbstractNodeExecutor implements NodeExecutor {

    /**
     * 获取依赖节点的输出（列表形式）
     */
    protected List<Object> getDependencyOutputs(Node node, PipelineContext context) {
        List<String> dependsOn = node.getDependsOn();
        if (dependsOn == null || dependsOn.isEmpty()) {
            return List.of();
        }
        return dependsOn.stream()
                .map(context::getNodeOutput)
                .toList();
    }

    /**
     * 获取单个依赖节点的输出（当只有一个依赖时使用）
     */
    protected Object getFirstDependencyOutput(Node node, PipelineContext context) {
        List<Object> outputs = getDependencyOutputs(node, context);
        return outputs.isEmpty() ? null : outputs.get(0);
    }

    /**
     * 带重试的执行包装
     */
    protected Object executeWithRetry(Node node, PipelineContext context, Map<String, Object> params) throws Exception {
        int retry = node.getRetryCount() != null ? node.getRetryCount() : 0;
        Exception lastException = null;
        for (int i = 0; i <= retry; i++) {
            try {
                return doExecute(node, context, params);
            } catch (Exception e) {
                lastException = e;
                log.warn("节点 {} 执行失败，重试 {}/{}", node.getId(), i, retry, e);
                if (i < retry) {
                    Thread.sleep(1000L * (i + 1)); // 简单退避
                }
            }
        }
        throw new RuntimeException("节点重试后仍执行失败", lastException);
    }

    /**
     * 子类实现具体执行逻辑
     */
    protected abstract Object doExecute(Node node, PipelineContext context, Map<String, Object> params) throws Exception;

    @Override
    public Object execute(Node node, PipelineContext context, Map<String, Object> params) throws Exception {
        log.info("开始执行节点: id={}, type={}", node.getId(), node.getType());
        long start = System.currentTimeMillis();
        try {
            Object result = executeWithRetry(node, context, params);
            long cost = System.currentTimeMillis() - start;
            log.info("节点 {} 执行成功，耗时={}ms", node.getId(), cost);
            return result;
        } catch (Exception e) {
            log.error("节点 {} 执行失败", node.getId(), e);
            if (node.getIgnoreFailure() != null && node.getIgnoreFailure()) {
                log.warn("节点 {} 失败已忽略", node.getId());
                return null;
            }
            throw e;
        }
    }
}
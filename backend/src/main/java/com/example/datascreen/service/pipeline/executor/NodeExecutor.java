package com.example.datascreen.service.pipeline.executor;


import com.example.datascreen.model.Node;
import com.example.datascreen.service.pipeline.PipelineContext;

import java.util.Map;

/**
 * 节点执行器接口
 */
public interface NodeExecutor {

    /**
     * 执行节点
     * @param node 当前节点
     * @param context 全局上下文（存储各节点输出）
     * @param params 额外参数（如 Pipeline 入参）
     * @return 节点执行结果（会存入 context）
     * @throws Exception 执行异常
     */
    Object execute(Node node, PipelineContext context, Map<String, Object> params) throws Exception;

    /**
     * 判断是否支持该节点类型
     */
    boolean supports(String nodeType);
}
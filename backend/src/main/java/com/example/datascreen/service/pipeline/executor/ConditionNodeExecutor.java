package com.example.datascreen.service.pipeline.executor;

import com.example.datascreen.model.Node;
import com.example.datascreen.model.NodeType;
import com.example.datascreen.service.pipeline.PipelineContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;
import java.util.Map;

/**
 * 条件节点执行器：根据表达式判断是否分支。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConditionNodeExecutor extends AbstractNodeExecutor {


    @Override
    protected Object doExecute(Node node, PipelineContext context, Map<String, Object> params) {
        String expression = node.getConfigString("expression");
        if (expression == null) {
            throw new IllegalArgumentException("条件节点必须配置 expression");
        }
        // 获取输入（依赖节点的输出）
        Object input = getFirstDependencyOutput(node, context);
        
        // 使用简单表达式求值（可用 Groovy 或 Spring EL）
        boolean result = evaluateExpression(expression, input, params, context.getGlobalVariables());
        
        // 将分支结果存入 context，供下游节点判断使用
        String targetNode = result ?
                node.getConfigString("trueBranch") : node.getConfigString("falseBranch");

        // 可以返回一个特殊对象，表示条件结果
        return Map.of("branch", result, "targetNode", targetNode);
    }

    private boolean evaluateExpression(String expr, Object input, Map<String, Object> params, Map<String, Object> global) {
        // 简化实现：用 Groovy 脚本引擎
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("groovy");
        engine.put("input", input);
        engine.put("params", params);
        engine.put("global", global);
        try {
            Object value = engine.eval(expr);
            return Boolean.TRUE.equals(value);
        } catch (Exception e) {
            throw new RuntimeException("条件表达式计算失败: " + expr, e);
        }
    }

    @Override
    public NodeType type() {
        return NodeType.CONDITION;
    }
}
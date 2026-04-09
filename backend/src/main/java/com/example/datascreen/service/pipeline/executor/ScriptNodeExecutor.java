package com.example.datascreen.service.pipeline.executor;


import com.example.datascreen.model.Node;
import com.example.datascreen.model.NodeType;
import com.example.datascreen.service.pipeline.PipelineContext;
import com.example.datascreen.service.pipeline.ScriptContext;
import com.example.datascreen.service.pipeline.ScriptEngineManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 脚本节点执行器：根据配置的脚本语言和源代码执行脚本。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScriptNodeExecutor extends AbstractNodeExecutor {

    private final ScriptEngineManager scriptEngineManager;


    @Override
    protected Object doExecute(Node node, PipelineContext context, Map<String, Object> params) throws Exception {
        String language = node.getConfigString("language");
        String source = node.getConfigString("source");
        if (language == null || source == null) {
            throw new IllegalArgumentException("脚本节点需要配置 language 和 source");
        }

        // 单个依赖：input 为该节点输出；多个依赖：input 为按 dependsOn 顺序的数组
        Object input = resolveScriptInput(node, context);
        // 构建脚本执行上下文
        ScriptContext scriptCtx = new ScriptContext();
        scriptCtx.setInput(input);
        scriptCtx.setParams(params);
        scriptCtx.setGlobalContext(context.getGlobalVariables());

        Object result = scriptEngineManager.execute(language, source, scriptCtx);
        log.debug("脚本节点 {} 执行完成，结果类型: {}", node.getId(),
                  result != null ? result.getClass().getSimpleName() : "null");
        return result;
    }

    @Override
    public NodeType type() {
        return NodeType.SCRIPT;
    }

    private static Object resolveScriptInput(Node node, PipelineContext context) {
        List<String> deps = node.getDependsOn();
        if (deps == null || deps.isEmpty()) {
            return null;
        }
        if (deps.size() == 1) {
            return context.getNodeOutput(deps.get(0));
        }
        return deps.stream().map(context::getNodeOutput).toList();
    }
}
package com.example.datascreen.service.pipeline.executor;


import com.example.datascreen.model.Node;
import com.example.datascreen.service.pipeline.PipelineContext;
import com.example.datascreen.service.pipeline.ScriptContext;
import com.example.datascreen.service.pipeline.ScriptEngineManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScriptNodeExecutor extends AbstractNodeExecutor {

    private final ScriptEngineManager scriptEngineManager;

    @Override
    public boolean supports(String nodeType) {
        return "script".equals(nodeType);
    }

    @Override
    protected Object doExecute(Node node, PipelineContext context, Map<String, Object> params) throws Exception {
        String language = node.getConfigString("language");
        String source = node.getConfigString("source");
        if (language == null || source == null) {
            throw new IllegalArgumentException("脚本节点需要配置 language 和 source");
        }

        // 获取输入：依赖节点的输出（如果依赖多个，可打包成列表）
        Object input = getFirstDependencyOutput(node, context);
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
}
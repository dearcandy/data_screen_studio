package com.example.datascreen.service.pipeline.executor;


import com.example.datascreen.model.Node;
import com.example.datascreen.service.pipeline.PipelineContext;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class OutputNodeExecutor extends AbstractNodeExecutor {

    @Override
    public boolean supports(String nodeType) {
        return "output".equals(nodeType);
    }

    @Override
    protected Object doExecute(Node node, PipelineContext context, Map<String, Object> params) {
        // 输出节点通常只是透传依赖节点的结果
        Object input = getFirstDependencyOutput(node, context);
        // 可以格式化输出（如 JSON）
        String format = node.getConfigString("format");
        if ("json".equals(format) && input != null) {
            // 转换为 JSON 字符串（使用 Jackson）
            return input; // 实际会序列化
        }
        return input;
    }
}
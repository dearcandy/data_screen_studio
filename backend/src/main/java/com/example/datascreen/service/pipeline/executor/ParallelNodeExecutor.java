package com.example.datascreen.service.pipeline.executor;


import com.example.datascreen.model.Node;
import com.example.datascreen.model.NodeType;
import com.example.datascreen.service.pipeline.PipelineContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ParallelNodeExecutor extends AbstractNodeExecutor {

    private final ObjectProvider<NodeExecutorRegistry> executorRegistryProvider;
    private final ExecutorService parallelExecutor = Executors.newCachedThreadPool();

    @Override
    public boolean supports(String nodeType) {
        return "parallel".equals(nodeType);
    }

    @Override
    protected Object doExecute(Node node, PipelineContext context, Map<String, Object> params) throws Exception {
        // 获取 branches 配置
        List<Map<String, Object>> branches = node.getConfigList("branches");
        if (branches == null || branches.isEmpty()) {
            throw new IllegalArgumentException("并行节点必须配置 branches");
        }
        String mergeStrategy = node.getConfigString("mergeStrategy");
        if (mergeStrategy == null) {
            mergeStrategy = "mergeToList";
        }

        // 获取输入（依赖节点的输出）
        Object input = getFirstDependencyOutput(node, context);

        // 并发执行每个分支（每个分支本身是一个子 Pipeline 或节点定义）
        List<CompletableFuture<Object>> futures = new ArrayList<>();
        for (Map<String, Object> branchConfig : branches) {
            CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
                try {
                    // 将 branchConfig 转换为 Node 对象（简易转换）
                    Node branchNode = convertToNode(branchConfig);
                    // 这里需要递归调用执行器，可以注入 NodeExecutorRegistry
                    // 简化：直接调用对应执行器
                    return executeBranch(branchNode, input, context, params);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, parallelExecutor);
            futures.add(future);
        }

        // 等待所有完成
        List<Object> branchResults = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        // 根据合并策略合并结果
        return mergeResults(branchResults, mergeStrategy);
    }

    private Node convertToNode(Map<String, Object> branchConfig) {
        Node node = new Node();
        node.setId((String) branchConfig.get("id"));
        Object t = branchConfig.get("type");
        node.setType(NodeType.fromCode(t != null ? t.toString() : "script"));
        node.setConfig((Map<String, Object>) branchConfig.get("config"));
        return node;
    }

    private Object executeBranch(Node branchNode, Object input, PipelineContext parentContext, Map<String, Object> params) {
        // 创建子上下文，继承父上下文的部分数据
        PipelineContext subContext = new PipelineContext();
        subContext.setNodeOutput("input", input);
        // 使用 NodeExecutorRegistry 获取执行器并执行
        // 这里简化：直接调用对应的 executor
        NodeExecutor executor = getExecutor(branchNode.getType().getCode());
        try {
            return executor.execute(branchNode, subContext, params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object mergeResults(List<Object> results, String strategy) {
        return switch (strategy) {
            case "mergeToList" -> results;
            case "mergeToMap" -> {
                Map<String, Object> map = new java.util.HashMap<>();
                for (int i = 0; i < results.size(); i++) {
                    map.put("branch_" + i, results.get(i));
                }
                yield map;
            }
            default -> results;
        };
    }

    private NodeExecutor getExecutor(String type) {
        NodeExecutorRegistry registry = executorRegistryProvider.getIfAvailable();
        if (registry == null) {
            throw new IllegalStateException("节点执行器注册表不可用");
        }
        return registry.getExecutor(type);
    }
}
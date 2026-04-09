package com.example.datascreen.service;

import com.example.datascreen.dto.ExecuteRequest;
import com.example.datascreen.dto.ExecuteResponse;
import com.example.datascreen.dto.PipelineDefinition;
import com.example.datascreen.entity.PipelineEntity;
import com.example.datascreen.entity.PipelineExecutionRecord;
import com.example.datascreen.model.Node;
import com.example.datascreen.service.pipeline.PipelineContext;
import com.example.datascreen.service.pipeline.executor.NodeExecutor;
import com.example.datascreen.service.pipeline.executor.NodeExecutorRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class PipelineEngine {

    private final PipelineService pipelineService;
    private final NodeExecutorRegistry executorRegistry;
    private final ObjectMapper objectMapper;

    // 存储异步执行任务，用于取消
    private final Map<String, Future<?>> runningTasks = new ConcurrentHashMap<>();
    private final ExecutorService asyncExecutor = Executors.newCachedThreadPool();

    /**
     * 执行 Pipeline（支持同步/异步）
     */
    public ExecuteResponse execute(ExecuteRequest request) {
        return executeInternal(request, true);
    }

    private ExecuteResponse executeInternal(ExecuteRequest request, boolean publishedOnly) {
        Long pipelineId = request.getPipelineId();
        Map<String, Object> inputParams = request.getInputParams() != null ? request.getInputParams() : Map.of();
        boolean async = request.isAsync();

        // 1. 加载 Pipeline 定义
        PipelineEntity pipeline = publishedOnly ? pipelineService.getPublishedPipeline(pipelineId) : pipelineService.getById(pipelineId);
        if (pipeline == null) {
            throw new IllegalArgumentException(publishedOnly
                    ? ("流程不存在或未发布: " + pipelineId)
                    : ("流程不存在: " + pipelineId));
        }
        List<String> validationErrors = pipelineService.validatePipeline(pipeline.getDefinitionJson());
        if (!validationErrors.isEmpty()) {
            throw new IllegalArgumentException("流程定义不合法: " + validationErrors.get(0));
        }

        // 2. 解析定义，构建 DAG
        PipelineDefinition definition = parseDefinition(pipeline.getDefinitionJson());

        // 3. 创建执行记录
        String executionId = UUID.randomUUID().toString();
        PipelineExecutionRecord record = new PipelineExecutionRecord();
        record.setExecutionId(executionId);
        record.setPipelineId(pipelineId);
        record.setTriggerType(request.getTriggerType() != null ? request.getTriggerType() : "manual");
        record.setInputParams(writeValueAsString(inputParams));
        record.setStatus("pending");
        record.setStartTime(Instant.now());
        pipelineService.saveExecutionRecord(record);

        // 4. 执行
        if (async) {
            Future<?> future = asyncExecutor.submit(() -> doExecute(definition, inputParams, executionId));
            runningTasks.put(executionId, future);
            return ExecuteResponse.async(executionId);
        } else {
            Object result = doExecute(definition, inputParams, executionId);
            return ExecuteResponse.sync(executionId, result);
        }
    }

    /**
     * 实际执行逻辑（同步）
     */
    private Object doExecute(PipelineDefinition definition, Map<String, Object> inputParams, String executionId) {
        log.info("开始执行流程，执行ID={}", executionId);
        updateExecutionStatus(executionId, "running", null, null);

        PipelineContext context = new PipelineContext();
        context.setGlobalVariables(new ConcurrentHashMap<>());
        // 将输入参数放入上下文，作为全局变量
        context.getGlobalVariables().putAll(inputParams);

        // 拓扑排序得到执行顺序
        List<Node> sortedNodes = topologicalSort(definition.getNodes());

        try {
            for (Node node : sortedNodes) {
                log.debug("执行节点: {}", node.getId());
                // 获取执行器
                NodeExecutor executor = executorRegistry.getExecutor(node.getType().getCode());
                // 执行节点
                Object output = executor.execute(node, context, inputParams);
                // 存储输出到上下文
                context.setNodeOutput(node.getId(), output);
            }
            // 获取最终输出（通常最后一个节点的输出，或指定的输出节点）
            Object finalResult = extractFinalOutput(definition, context);
            updateExecutionStatus(executionId, "success", finalResult, null);
            log.info("流程执行成功，执行ID={}", executionId);
            return finalResult;
        } catch (Exception e) {
            log.error("流程执行失败，执行ID={}", executionId, e);
            updateExecutionStatus(executionId, "failed", null, e.getMessage());
            throw new RuntimeException("流程执行失败", e);
        } finally {
            runningTasks.remove(executionId);
        }
    }

    /**
     * 拓扑排序（Kahn 算法）
     */
    private List<Node> topologicalSort(List<Node> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException("流程节点不能为空");
        }
        Map<String, Node> nodeMap = new HashMap<>();
        for (Node node : nodes) {
            if (node.getId() == null || node.getId().isBlank()) {
                throw new IllegalArgumentException("节点ID不能为空");
            }
            if (node.getType() == null) {
                throw new IllegalArgumentException("节点类型不能为空: " + node.getId());
            }
            if (nodeMap.containsKey(node.getId())) {
                throw new IllegalArgumentException("存在重复节点ID: " + node.getId());
            }
            nodeMap.put(node.getId(), node);
        }
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adjacency = new HashMap<>();
        for (Node node : nodes) {
            inDegree.put(node.getId(), 0);
            adjacency.put(node.getId(), new ArrayList<>());
        }
        for (Node node : nodes) {
            if (node.getDependsOn() != null) {
                for (String depId : node.getDependsOn()) {
                    if (!nodeMap.containsKey(depId)) {
                        throw new IllegalArgumentException("节点依赖不存在: " + depId);
                    }
                    adjacency.get(depId).add(node.getId());
                    inDegree.merge(node.getId(), 1, Integer::sum);
                }
            }
        }
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }
        List<Node> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            String nodeId = queue.poll();
            result.add(nodeMap.get(nodeId));
            for (String neighbor : adjacency.get(nodeId)) {
                inDegree.merge(neighbor, -1, Integer::sum);
                if (inDegree.get(neighbor) == 0) {
                    queue.offer(neighbor);
                }
            }
        }
        if (result.size() != nodes.size()) {
            throw new IllegalStateException("流程DAG存在环依赖");
        }
        return result;
    }

    /**
     * 提取最终输出：若有 output 类型节点，取最后一个 output 节点的输出；否则取最后执行的节点输出
     */
    private Object extractFinalOutput(PipelineDefinition definition, PipelineContext context) {
        List<Node> outputNodes = definition.getNodes().stream()
                .filter(n -> n.getType() == com.example.datascreen.model.NodeType.OUTPUT)
                .toList();
        if (!outputNodes.isEmpty()) {
            Node lastOutput = outputNodes.get(outputNodes.size() - 1);
            return context.getNodeOutput(lastOutput.getId());
        }
        // 没有 output 节点，返回最后一个节点的输出
        List<Node> allNodes = definition.getNodes();
        if (allNodes.isEmpty()) return null;
        Node lastNode = allNodes.get(allNodes.size() - 1);
        return context.getNodeOutput(lastNode.getId());
    }

    private PipelineDefinition parseDefinition(String definitionJson) {
        try {
            return objectMapper.readValue(definitionJson, PipelineDefinition.class);
        } catch (Exception e) {
            throw new RuntimeException("流程定义解析失败", e);
        }
    }

    private void updateExecutionStatus(String executionId, String status, Object result, String errorMsg) {
        PipelineExecutionRecord record = pipelineService.getExecutionRecord(executionId);
        if (record == null) return;
        record.setStatus(status);
        if (result != null) {
            record.setOutputResult(writeValueAsString(result));
        }
        if (errorMsg != null) {
            record.setErrorMsg(errorMsg);
        }
        record.setEndTime(Instant.now());
        pipelineService.updateExecutionRecord(record);
    }

    private String writeValueAsString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("对象序列化失败", e);
            return obj != null ? obj.toString() : null;
        }
    }

    /**
     * 取消执行中的任务
     */
    public void cancel(String executionId) {
        Future<?> future = runningTasks.get(executionId);
        if (future != null && !future.isDone()) {
            boolean cancelled = future.cancel(true);
            if (cancelled) {
                updateExecutionStatus(executionId, "cancelled", null, "用户取消执行");
                log.info("流程执行已取消，执行ID={}", executionId);
            }
        } else {
            throw new IllegalArgumentException("执行记录不存在或已结束: " + executionId);
        }
    }

    public Object executeSync(Long pipelineId, Map<String, Object> params) {
        ExecuteRequest request = new ExecuteRequest();
        request.setPipelineId(pipelineId);
        request.setInputParams(params);
        request.setAsync(false);
        request.setTriggerType("manual");
        ExecuteResponse response = execute(request);
        return response.getResult();
    }

    public String executeAsync(Long pipelineId, Map<String, Object> params) {
        ExecuteRequest request = new ExecuteRequest();
        request.setPipelineId(pipelineId);
        request.setInputParams(params);
        request.setAsync(true);
        request.setTriggerType("manual");
        ExecuteResponse response = execute(request);
        return response.getExecutionId();
    }

    public Object previewById(Long pipelineId, Map<String, Object> params) {
        ExecuteRequest request = new ExecuteRequest();
        request.setPipelineId(pipelineId);
        request.setInputParams(params);
        request.setAsync(false);
        request.setTriggerType("preview");
        ExecuteResponse response = executeInternal(request, false);
        return response.getResult();
    }

    public Object executeByPublicToken(String token, Map<String, Object> params) {
        PipelineEntity pipeline = pipelineService.findByPublicToken(token);
        if (pipeline == null) {
            throw new IllegalArgumentException("无效的流程令牌");
        }
        if (!pipeline.isExternalEnabled()) {
            throw new IllegalStateException("流程外部调用未开启");
        }
        List<String> validationErrors = pipelineService.validatePipeline(pipeline.getDefinitionJson());
        if (!validationErrors.isEmpty()) {
            throw new IllegalArgumentException("流程定义不合法: " + validationErrors.get(0));
        }

        PipelineDefinition definition = parseDefinition(pipeline.getDefinitionJson());
        String executionId = UUID.randomUUID().toString();
        PipelineExecutionRecord record = new PipelineExecutionRecord();
        record.setExecutionId(executionId);
        record.setPipelineId(pipeline.getId());
        record.setTriggerType("external");
        record.setInputParams(writeValueAsString(params != null ? params : Map.of()));
        record.setStatus("pending");
        record.setStartTime(Instant.now());
        pipelineService.saveExecutionRecord(record);
        return doExecute(definition, params != null ? params : Map.of(), executionId);
    }
}
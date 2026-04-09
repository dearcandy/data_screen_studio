package com.example.datascreen.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.datascreen.dto.PageResult;
import com.example.datascreen.dto.PipelineDefinition;
import com.example.datascreen.dto.SavePipelineRequest;
import com.example.datascreen.entity.PipelineEntity;
import com.example.datascreen.entity.PipelineExecutionRecord;
import com.example.datascreen.model.Node;
import com.example.datascreen.model.NodeType;
import com.example.datascreen.repository.PipelineExecutionMapper;
import com.example.datascreen.repository.PipelineRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PipelineService {

    private final PipelineRepository pipelineRepository;
    private final PipelineExecutionMapper executionMapper;
    private final ObjectMapper objectMapper;

    /**
     * 保存pipeline
     */
    public Long savePipeline(SavePipelineRequest request) {
        if (request.getId() == null) {
            PipelineEntity entity = new PipelineEntity();
            entity.setName(request.getName());
            entity.setDescription(request.getDescription());
            entity.setDefinitionJson(request.getDefinitionJson());
            entity.setStatus(request.getStatus() != null ? request.getStatus() : "draft");
            if (entity.getPublicToken() == null || entity.getPublicToken().isBlank()) {
                entity.setPublicToken(UUID.randomUUID().toString().replace("-", ""));
            }
            pipelineRepository.insert(entity);
            return entity.getId();
        }
        PipelineEntity entity = pipelineRepository.selectById(request.getId());
        if (entity == null) {
            throw new IllegalArgumentException("流程不存在");
        }
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setDefinitionJson(request.getDefinitionJson());
        entity.setStatus(request.getStatus() != null ? request.getStatus() : entity.getStatus());
        pipelineRepository.updateById(entity);
        return entity.getId();
    }

    /**
     * 获取pipeline
     */
    public PipelineEntity getById(Long id) {
        return pipelineRepository.selectById(id);
    }

    /**
     * 分页获取pipeline
     */
    public PageResult<PipelineEntity> listPipelines(int page, int size, String keyword) {
        LambdaQueryWrapper<PipelineEntity> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.like(PipelineEntity::getName, keyword.trim());
        }
        qw.orderByDesc(PipelineEntity::getId);
        List<PipelineEntity> all = pipelineRepository.selectList(qw);
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int from = Math.min((safePage - 1) * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());

        PageResult<PipelineEntity> result = new PageResult<>();
        result.setTotal(all.size());
        result.setRecords(new ArrayList<>(all.subList(from, to)));
        result.setPage(safePage);
        result.setSize(safeSize);
        return result;
    }

    public void deletePipeline(Long id) {
        pipelineRepository.deleteById(id);
    }

    public void publishPipeline(Long id) {
        PipelineEntity entity = pipelineRepository.selectById(id);
        if (entity == null) throw new RuntimeException("流程不存在");
        entity.setStatus("published");
        pipelineRepository.updateById(entity);
    }

    public PipelineEntity findByPublicToken(String token) {
        return pipelineRepository.findByPublicToken(token);
    }

    public PipelineEntity regenerateToken(Long id) {
        PipelineEntity entity = pipelineRepository.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("流程不存在");
        }
        entity.setPublicToken(UUID.randomUUID().toString().replace("-", ""));
        pipelineRepository.updateById(entity);
        return entity;
    }

    public PipelineEntity setExternalEnabled(Long id, boolean enabled) {
        PipelineEntity entity = pipelineRepository.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("流程不存在");
        }
        entity.setExternalEnabled(enabled);
        pipelineRepository.updateById(entity);
        return entity;
    }

    public PipelineEntity getPublishedPipeline(Long id) {
        PipelineEntity entity = pipelineRepository.selectById(id);
        if (entity == null) {
            return null;
        }
        return "published".equalsIgnoreCase(entity.getStatus()) ? entity : null;
    }

    public PipelineExecutionRecord getExecutionRecord(String executionId) {
        return executionMapper.selectOne(
                new LambdaQueryWrapper<PipelineExecutionRecord>()
                        .eq(PipelineExecutionRecord::getExecutionId, executionId)
                        .last("limit 1")
        );
    }

    public List<PipelineExecutionRecord> getExecutionHistory(Long pipelineId, int page, int size) {
        LambdaQueryWrapper<PipelineExecutionRecord> qw = new LambdaQueryWrapper<PipelineExecutionRecord>()
                .eq(PipelineExecutionRecord::getPipelineId, pipelineId)
                .orderByDesc(PipelineExecutionRecord::getId);
        List<PipelineExecutionRecord> all = executionMapper.selectList(qw);
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int from = Math.min((safePage - 1) * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        return new ArrayList<>(all.subList(from, to));
    }

    public void saveExecutionRecord(PipelineExecutionRecord record) {
        executionMapper.insert(record);
    }

    public void updateExecutionRecord(PipelineExecutionRecord record) {
        executionMapper.updateById(record);
    }

    public List<String> validatePipeline(String definitionJson) {
        List<String> errors = new ArrayList<>();
        if (definitionJson == null || definitionJson.isBlank()) {
            return List.of("definitionJson 不能为空");
        }
        PipelineDefinition definition;
        try {
            definition = objectMapper.readValue(definitionJson, PipelineDefinition.class);
        } catch (Exception e) {
            return List.of("definitionJson 解析失败: " + e.getMessage());
        }
        List<Node> nodes = definition.getNodes();
        if (nodes == null || nodes.isEmpty()) {
            return List.of("nodes 不能为空");
        }

        Map<String, Node> nodeById = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            if (node == null) {
                errors.add("第 " + (i + 1) + " 个节点不能为空");
                continue;
            }
            String id = node.getId();
            if (id == null || id.isBlank()) {
                errors.add("第 " + (i + 1) + " 个节点 id 不能为空");
                continue;
            }
            if (nodeById.containsKey(id)) {
                errors.add("节点 id 重复: " + id);
                continue;
            }
            nodeById.put(id, node);

            if (node.getType() == null) {
                errors.add("节点 type 不能为空: " + id);
            } else {
                validateNodeConfig(node, errors);
            }
        }

        for (Node node : nodes) {
            if (node == null || node.getId() == null || node.getId().isBlank()) {
                continue;
            }
            List<String> deps = node.getDependsOn();
            if (deps == null) {
                continue;
            }
            Set<String> seen = new HashSet<>();
            for (String dep : deps) {
                if (dep == null || dep.isBlank()) {
                    errors.add("节点 " + node.getId() + " 的 dependsOn 不能包含空值");
                    continue;
                }
                if (!nodeById.containsKey(dep)) {
                    errors.add("节点 " + node.getId() + " 依赖不存在: " + dep);
                }
                if (dep.equals(node.getId())) {
                    errors.add("节点 " + node.getId() + " 不能依赖自身");
                }
                if (!seen.add(dep)) {
                    errors.add("节点 " + node.getId() + " 的 dependsOn 存在重复项: " + dep);
                }
            }
        }

        errors.addAll(checkCycle(nodes, nodeById));

        for (Node node : nodes) {
            if (node == null || node.getType() != NodeType.CONDITION) {
                continue;
            }
            String trueBranch = node.getConfigString("trueBranch");
            String falseBranch = node.getConfigString("falseBranch");
            if (trueBranch != null && !trueBranch.isBlank() && !nodeById.containsKey(trueBranch)) {
                errors.add("condition 节点 " + node.getId() + " 的 trueBranch 不存在: " + trueBranch);
            }
            if (falseBranch != null && !falseBranch.isBlank() && !nodeById.containsKey(falseBranch)) {
                errors.add("condition 节点 " + node.getId() + " 的 falseBranch 不存在: " + falseBranch);
            }
        }
        return errors;
    }

    private void validateNodeConfig(Node node, List<String> errors) {
        String id = node.getId();
        NodeType type = node.getType();
        switch (type) {
            case DATA_SOURCE -> {
                if (node.getConfigLong("dataSourceId") == null) {
                    errors.add("dataSource 节点缺少 dataSourceId: " + id);
                }
            }
            case DATA_SET -> {
                if (node.getConfigLong("dataSetId") == null) {
                    errors.add("dataSet 节点缺少 dataSetId: " + id);
                }
            }
            case FETCH -> {
                if (node.getConfigLong("dataSourceId") == null) {
                    errors.add("fetch 节点缺少 dataSourceId: " + id);
                }
                if (node.getConfigString("fetchSpec") == null || node.getConfigString("fetchSpec").isBlank()) {
                    errors.add("fetch 节点缺少 fetchSpec: " + id);
                }
            }
            case SCRIPT -> {
                if (node.getConfigString("language") == null || node.getConfigString("language").isBlank()) {
                    errors.add("script 节点缺少 language: " + id);
                }
                if (node.getConfigString("source") == null || node.getConfigString("source").isBlank()) {
                    errors.add("script 节点缺少 source: " + id);
                }
            }
            case CONDITION -> {
                if (node.getConfigString("expression") == null || node.getConfigString("expression").isBlank()) {
                    errors.add("condition 节点缺少 expression: " + id);
                }
                if (node.getConfigString("trueBranch") == null || node.getConfigString("trueBranch").isBlank()) {
                    errors.add("condition 节点缺少 trueBranch: " + id);
                }
                if (node.getConfigString("falseBranch") == null || node.getConfigString("falseBranch").isBlank()) {
                    errors.add("condition 节点缺少 falseBranch: " + id);
                }
            }
            case PARALLEL -> {
                List<Map<String, Object>> branches = node.getConfigList("branches");
                if (branches == null || branches.isEmpty()) {
                    errors.add("parallel 节点缺少 branches: " + id);
                    break;
                }
                for (int i = 0; i < branches.size(); i++) {
                    Map<String, Object> branch = branches.get(i);
                    if (branch == null) {
                        errors.add("parallel 节点 " + id + " 的第 " + (i + 1) + " 个 branch 不能为空");
                        continue;
                    }
                    Object bid = branch.get("id");
                    Object btype = branch.get("type");
                    if (bid == null || bid.toString().isBlank()) {
                        errors.add("parallel 节点 " + id + " 的 branch[" + i + "] 缺少 id");
                    }
                    if (btype == null || btype.toString().isBlank()) {
                        errors.add("parallel 节点 " + id + " 的 branch[" + i + "] 缺少 type");
                        continue;
                    }
                    if (!"script".equals(btype.toString())) {
                        errors.add("parallel 节点 " + id + " 仅支持 script branch，当前为: " + btype);
                    }
                    Object rawConfig = branch.get("config");
                    if (!(rawConfig instanceof Map<?, ?> cfg)) {
                        errors.add("parallel 节点 " + id + " 的 branch[" + i + "] 缺少 config");
                    } else {
                        Object source = cfg.get("source");
                        Object language = cfg.get("language");
                        if (source == null || source.toString().isBlank()) {
                            errors.add("parallel 节点 " + id + " 的 branch[" + i + "] 缺少 source");
                        }
                        if (language == null || language.toString().isBlank()) {
                            errors.add("parallel 节点 " + id + " 的 branch[" + i + "] 缺少 language");
                        }
                    }
                }
            }
            case OUTPUT, START, END -> {
                // 当前无需额外配置
            }
        }
    }

    private List<String> checkCycle(List<Node> nodes, Map<String, Node> nodeById) {
        List<String> errors = new ArrayList<>();
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adjacency = new HashMap<>();

        for (Node node : nodes) {
            if (node == null || node.getId() == null || node.getId().isBlank()) {
                continue;
            }
            inDegree.putIfAbsent(node.getId(), 0);
            adjacency.putIfAbsent(node.getId(), new ArrayList<>());
        }
        for (Node node : nodes) {
            if (node == null || node.getId() == null || node.getId().isBlank()) {
                continue;
            }
            if (node.getDependsOn() == null) {
                continue;
            }
            for (String dep : node.getDependsOn()) {
                if (!nodeById.containsKey(dep)) {
                    continue;
                }
                adjacency.get(dep).add(node.getId());
                inDegree.put(node.getId(), inDegree.getOrDefault(node.getId(), 0) + 1);
            }
        }

        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> e : inDegree.entrySet()) {
            if (e.getValue() == 0) {
                queue.offer(e.getKey());
            }
        }
        int visited = 0;
        while (!queue.isEmpty()) {
            String id = queue.poll();
            visited++;
            for (String next : adjacency.getOrDefault(id, List.of())) {
                int deg = inDegree.get(next) - 1;
                inDegree.put(next, deg);
                if (deg == 0) {
                    queue.offer(next);
                }
            }
        }
        if (visited != inDegree.size()) {
            errors.add("DAG 存在环依赖");
        }
        return errors;
    }
}
package com.example.datascreen.web;

import com.example.datascreen.dto.ExecutePipelineRequest;
import com.example.datascreen.dto.PageResult;
import com.example.datascreen.dto.SavePipelineRequest;
import com.example.datascreen.dto.ValidatePipelineRequest;
import com.example.datascreen.entity.PipelineEntity;
import com.example.datascreen.entity.PipelineExecutionRecord;
import com.example.datascreen.service.PipelineEngine;
import com.example.datascreen.service.PipelineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;

/**
 * Pipeline 管理及执行 Controller
 */
@RestController
@RequestMapping("/api/pipeline")
@RequiredArgsConstructor
@Slf4j
public class PipelineController {

    private final PipelineService pipelineService;
    private final PipelineEngine pipelineEngine;

    /**
     * 创建或更新流程定义
     * @param request 流程定义
     * @return 流程ID
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> savePipeline(@Valid @RequestBody SavePipelineRequest request) {
        Long id = pipelineService.savePipeline(request);
        return ResponseEntity.ok(Map.of("id", id, "success", true));
    }

    /**
     * 根据 ID 查询流程定义
     * @param id 流程ID
     * @return 流程定义
     */
    @GetMapping("/{id}")
    public ResponseEntity<PipelineEntity> getPipeline(@PathVariable Long id) {
        PipelineEntity entity = pipelineService.getById(id);
        return ResponseEntity.ok(entity);
    }

    /**
     * 分页查询流程列表
     * @param page 页码
     * @param size 每页数量
     * @param keyword 搜索关键词（可选）
     * @return 分页结果
     */
    @GetMapping("/list")
    public ResponseEntity<PageResult<PipelineEntity>> listPipelines(@RequestParam(defaultValue = "1") int page,
                                                                    @RequestParam(defaultValue = "10") int size,
                                                                    @RequestParam(required = false) String keyword) {
        PageResult<PipelineEntity> result = pipelineService.listPipelines(page, size, keyword);
        return ResponseEntity.ok(result);
    }

    /**
     * 删除流程（逻辑删除或物理删除）
     * @param id 流程ID
     * @return 无内容响应
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePipeline(@PathVariable Long id) {
        pipelineService.deletePipeline(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 发布流程（状态改为 published）
     * @param id 流程ID
     * @return 无内容响应
     */
    @PostMapping("/{id}/publish")
    public ResponseEntity<Void> publishPipeline(@PathVariable Long id) {
        pipelineService.publishPipeline(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 同步执行流程（适用于短时任务，直接返回结果）
     * @param request 执行请求
     * @return 执行结果
     */
    @PostMapping("/execute/sync")
    public ResponseEntity<Object> executeSync(@RequestBody @Valid ExecutePipelineRequest request) {
        long start = System.currentTimeMillis();
        try {
            Object result = pipelineEngine.executeSync(request.getPipelineId(), request.getParams());
            log.info("同步执行流程 {} 完成，耗时 {} ms", request.getPipelineId(), System.currentTimeMillis() - start);
            return ResponseEntity.ok(Map.of("success", true, "data", result));
        } catch (Exception e) {
            log.error("同步执行流程 {} 失败", request.getPipelineId(), e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * 预览执行流程（允许 draft 状态，用于编辑页调试）
     * @param id 流程ID
     * @param params 执行参数（可选）
     * @return 执行结果
     */
    @PostMapping("/{id}/preview")
    public ResponseEntity<Object> preview(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> params) {
        long start = System.currentTimeMillis();
        try {
            Object result = pipelineEngine.previewById(id, params != null ? params : Map.of());
            log.info("预览流程 {} 完成，耗时 {} ms", id, System.currentTimeMillis() - start);
            return ResponseEntity.ok(Map.of("success", true, "data", result));
        } catch (Exception e) {
            log.error("预览流程 {} 失败", id, e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * 异步执行流程（返回 executionId，轮询结果）
     * @param request 执行请求
     * @return 执行ID
     */
    @PostMapping("/execute/async")
    public ResponseEntity<Map<String, String>> executeAsync(@RequestBody @Valid ExecutePipelineRequest request) {
        String executionId = pipelineEngine.executeAsync(request.getPipelineId(), request.getParams());
        return ResponseEntity.ok(Map.of("executionId", executionId));
    }

    /**
     * 查询异步执行结果
     * @param executionId 执行ID
     * @return 执行记录
     * @throws IllegalArgumentException 如果执行ID不存在
     * @throws IllegalArgumentException 如果执行记录状态不是 completed 或 failed
     */
    @GetMapping("/execution/{executionId}")
    public ResponseEntity<PipelineExecutionRecord> getExecutionResult(@PathVariable String executionId) {
        PipelineExecutionRecord record = pipelineService.getExecutionRecord(executionId);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(record);
    }

    /**
     * 查询某个流程 的执行历史记录
     * @param pipelineId 流程ID
     * @param page 页码（默认 1）
     * @param size 每页数量（默认 10）
     * @return 执行记录列表
     */
    @GetMapping("/{pipelineId}/executions")
    public ResponseEntity<List<PipelineExecutionRecord>> getExecutionHistory(@PathVariable Long pipelineId,
                                                                              @RequestParam(defaultValue = "1") int page,
                                                                              @RequestParam(defaultValue = "10") int size) {
        List<PipelineExecutionRecord> records = pipelineService.getExecutionHistory(pipelineId, page, size);
        return ResponseEntity.ok(records);
    }

    /**
     * 重新生成流程的 API 密钥（用于外部调用）
     * @param id 流程ID
     * @return 重新生成的 API 密钥
     */
    @PostMapping("/{id}/regenerate-token")
    public ResponseEntity<PipelineEntity> regenerateToken(@PathVariable Long id) {
        return ResponseEntity.ok(pipelineService.regenerateToken(id));
    }


    /**
     * 设置流程是否启用外部调用（默认启用）
     * @param id 流程ID
     * @param req 包含 enabled 字段的 JSON 请求体
     * @return 更新后的流程实体
     */
    @PostMapping("/{id}/external-enabled")
    public ResponseEntity<PipelineEntity> setExternalEnabled(@PathVariable Long id,
                                                             @RequestBody Map<String, Object> req) {
        boolean enabled = Boolean.TRUE.equals(req.get("enabled"));
        return ResponseEntity.ok(pipelineService.setExternalEnabled(id, enabled));
    }

    /**
     * 验证流程定义（检查 DAG 是否有环、节点配置是否正确）
     * @param request 验证请求
     * @return 验证结果
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validatePipeline(@RequestBody @Valid ValidatePipelineRequest request) {
        List<String> errors = pipelineService.validatePipeline(request.getDefinitionJson());
        if (errors.isEmpty()) {
            return ResponseEntity.ok(Map.of("valid", true));
        } else {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "errors", errors));
        }
    }





}
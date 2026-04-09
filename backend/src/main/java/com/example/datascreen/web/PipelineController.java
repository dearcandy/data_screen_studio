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
     * 创建或更新 Pipeline 定义
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> savePipeline(@Valid @RequestBody SavePipelineRequest request) {
        Long id = pipelineService.savePipeline(request);
        return ResponseEntity.ok(Map.of("id", id, "success", true));
    }

    /**
     * 根据 ID 查询 Pipeline 定义
     */
    @GetMapping("/{id}")
    public ResponseEntity<PipelineEntity> getPipeline(@PathVariable Long id) {
        PipelineEntity entity = pipelineService.getById(id);
        return ResponseEntity.ok(entity);
    }

    /**
     * 分页查询 Pipeline 列表
     */
    @GetMapping("/list")
    public ResponseEntity<PageResult<PipelineEntity>> listPipelines(@RequestParam(defaultValue = "1") int page,
                                                                    @RequestParam(defaultValue = "10") int size,
                                                                    @RequestParam(required = false) String keyword) {
        PageResult<PipelineEntity> result = pipelineService.listPipelines(page, size, keyword);
        return ResponseEntity.ok(result);
    }

    /**
     * 删除 Pipeline（逻辑删除或物理删除）
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePipeline(@PathVariable Long id) {
        pipelineService.deletePipeline(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 发布 Pipeline（状态改为 published）
     */
    @PostMapping("/{id}/publish")
    public ResponseEntity<Void> publishPipeline(@PathVariable Long id) {
        pipelineService.publishPipeline(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 同步执行 Pipeline（适用于短时任务，直接返回结果）
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
     * 预览执行（允许 draft 状态，用于编辑页调试）
     */
    @PostMapping("/{id}/preview")
    public ResponseEntity<Object> preview(@PathVariable Long id,
                                          @RequestBody(required = false) Map<String, Object> params) {
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
     * 异步执行 Pipeline（返回 executionId，轮询结果）
     */
    @PostMapping("/execute/async")
    public ResponseEntity<Map<String, String>> executeAsync(@RequestBody @Valid ExecutePipelineRequest request) {
        String executionId = pipelineEngine.executeAsync(request.getPipelineId(), request.getParams());
        return ResponseEntity.ok(Map.of("executionId", executionId));
    }

    /**
     * 查询异步执行结果
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
     * 查询某个 Pipeline 的执行历史
     */
    @GetMapping("/{pipelineId}/executions")
    public ResponseEntity<List<PipelineExecutionRecord>> getExecutionHistory(@PathVariable Long pipelineId,
                                                                              @RequestParam(defaultValue = "1") int page,
                                                                              @RequestParam(defaultValue = "10") int size) {
        List<PipelineExecutionRecord> records = pipelineService.getExecutionHistory(pipelineId, page, size);
        return ResponseEntity.ok(records);
    }

    @PostMapping("/{id}/regenerate-token")
    public ResponseEntity<PipelineEntity> regenerateToken(@PathVariable Long id) {
        return ResponseEntity.ok(pipelineService.regenerateToken(id));
    }

    @PostMapping("/{id}/external-enabled")
    public ResponseEntity<PipelineEntity> setExternalEnabled(@PathVariable Long id,
                                                             @RequestBody Map<String, Object> req) {
        boolean enabled = Boolean.TRUE.equals(req.get("enabled"));
        return ResponseEntity.ok(pipelineService.setExternalEnabled(id, enabled));
    }

    /**
     * 验证 Pipeline 定义（检查 DAG 是否有环、节点配置是否正确）
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
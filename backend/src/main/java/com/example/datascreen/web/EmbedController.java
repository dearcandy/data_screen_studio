package com.example.datascreen.web;

import com.example.datascreen.dto.ApiResponse;
import com.example.datascreen.service.DataSetService;
import com.example.datascreen.service.PipelineEngine;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 对外嵌入接口：无管理端 Cookie 依赖，通过数据集公开的 {@code token} 拉取加工后的数据。
 */
@RestController
@RequestMapping("/embed")
public class EmbedController {

    private final DataSetService executionService;
    private final PipelineEngine pipelineEngine;

    public EmbedController(DataSetService executionService, PipelineEngine pipelineEngine) {
        this.executionService = executionService;
        this.pipelineEngine = pipelineEngine;
    }

    /**
     * 大屏或第三方 GET 调用；{@code token} 对应表 {@code ds_data_set.public_token}。
     * 成功时 {@code data} 为脚本执行后的 JSON 可序列化结构。
     */
    @GetMapping("/data/{token}")
    public ApiResponse<Object> data(@PathVariable String token) throws Exception {
        Object data = executionService.executeByToken(token);
        return ApiResponse.ok(data);
    }

    /**
     * Pipeline 外部调用（GET 无参）
     */
    @GetMapping("/pipeline/{token}")
    public ApiResponse<Object> pipeline(@PathVariable String token) {
        Object data = pipelineEngine.executeByPublicToken(token, Map.of());
        return ApiResponse.ok(data);
    }

    /**
     * Pipeline 外部调用（POST 可传 params）
     */
    @PostMapping("/pipeline/{token}")
    public ApiResponse<Object> pipelinePost(@PathVariable String token,
                                            @RequestBody(required = false) Map<String, Object> params) {
        Object data = pipelineEngine.executeByPublicToken(token, params != null ? params : Map.of());
        return ApiResponse.ok(data);
    }
}

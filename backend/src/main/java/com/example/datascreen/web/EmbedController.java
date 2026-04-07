package com.example.datascreen.web;

import com.example.datascreen.dto.ApiResponse;
import com.example.datascreen.service.DataSetExecutionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对外嵌入接口：无管理端 Cookie 依赖，通过数据集公开的 {@code token} 拉取加工后的数据。
 */
@RestController
@RequestMapping("/embed")
public class EmbedController {

    private final DataSetExecutionService executionService;

    public EmbedController(DataSetExecutionService executionService) {
        this.executionService = executionService;
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
}

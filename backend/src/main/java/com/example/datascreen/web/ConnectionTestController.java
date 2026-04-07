package com.example.datascreen.web;

import com.example.datascreen.dto.ApiResponse;
import com.example.datascreen.dto.ConnectionTestRequest;
import com.example.datascreen.service.DataSourceConfigValidator;
import com.example.datascreen.service.source.DataSourceHandlerManager;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 连接探测
 */
@RestController
@RequestMapping("/api/connection")
public class ConnectionTestController {

    private final DataSourceConfigValidator configValidator;
    private final DataSourceHandlerManager handlerManager;

    public ConnectionTestController(DataSourceConfigValidator configValidator, DataSourceHandlerManager handlerManager) {
        this.configValidator = configValidator;
        this.handlerManager = handlerManager;
    }

    /**
     * 测试数据源连接
     * @param connectionTestRequest 测试请求
     * @return 测试结果
     * @throws Exception 测试异常
     */
    @PostMapping("/test")
    public ApiResponse<Void> test(@Valid @RequestBody ConnectionTestRequest connectionTestRequest) throws Exception {
        // 校验数据源配置
        JsonNode cfg = configValidator.validate(connectionTestRequest.getType(), connectionTestRequest.getConfigJson());
        // 测试数据源连接
        handlerManager.get(connectionTestRequest.getType()).testConnection(cfg);

        return ApiResponse.ok(null);
    }
}

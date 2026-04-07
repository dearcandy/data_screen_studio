package com.example.datascreen.web;

import com.example.datascreen.dto.ApiResponse;
import com.example.datascreen.dto.ConnectionTestRequest;
import com.example.datascreen.service.ConnectionTestService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 连接探测：按类型尝试 JDBC / HTTP / Redis / 文件等，不写入业务表。
 */
@RestController
@RequestMapping("/api/connection")
public class ConnectionTestController {

    private final ConnectionTestService connectionTestService;

    public ConnectionTestController(ConnectionTestService connectionTestService) {
        this.connectionTestService = connectionTestService;
    }

    /**
     * 根据 {@link ConnectionTestRequest#getType()} 与 {@link ConnectionTestRequest#getConfigJson()} 做一次连通性校验；
     * 成功时 {@code code=0} 且 {@code data} 为 null。
     */
    @PostMapping("/test")
    public ApiResponse<Void> test(@Valid @RequestBody ConnectionTestRequest req) throws Exception {
        connectionTestService.test(req.getType(), req.getConfigJson());
        return ApiResponse.ok(null);
    }
}

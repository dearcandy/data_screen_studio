package com.example.datascreen.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * 统一 API 响应体，供管理端与嵌入接口共用。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;
    private Map<String, Object> meta;

    /** ，自动附带 {@code meta.timestamp}。 */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "ok", data, Map.of("timestamp", Instant.now().toString()));
    }

    /** 成功响应，在默认 timestamp 基础上合并自定义 {@code meta}。 */
    public static <T> ApiResponse<T> ok(T data, Map<String, Object> meta) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("timestamp", Instant.now().toString());
        if (meta != null) {
            m.putAll(meta);
        }
        return new ApiResponse<>(0, "ok", data, m);
    }

    /** 失败响应，{@code data} 为 null。 */
    public static <T> ApiResponse<T> err(int code, String message) {
        return new ApiResponse<>(code, message, null, Map.of("timestamp", Instant.now().toString()));
    }
}

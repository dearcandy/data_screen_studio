package com.example.datascreen.dto;

import lombok.Data;

@Data
public class ExecuteResponse {
    private String executionId;
    private boolean async;
    private Object result;

    public static ExecuteResponse async(String executionId) {
        ExecuteResponse resp = new ExecuteResponse();
        resp.setExecutionId(executionId);
        resp.setAsync(true);
        return resp;
    }

    public static ExecuteResponse sync(String executionId, Object result) {
        ExecuteResponse resp = new ExecuteResponse();
        resp.setExecutionId(executionId);
        resp.setAsync(false);
        resp.setResult(result);
        return resp;
    }
}
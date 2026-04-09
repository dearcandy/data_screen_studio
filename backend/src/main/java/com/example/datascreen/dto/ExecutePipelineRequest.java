package com.example.datascreen.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ExecutePipelineRequest {

    @NotNull(message = "pipelineId 不能为空")
    private Long pipelineId;

    private Map<String, Object> params = new HashMap<>();
}
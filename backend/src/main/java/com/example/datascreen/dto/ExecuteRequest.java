package com.example.datascreen.dto;

import lombok.Data;
import java.util.Map;

@Data
public class ExecuteRequest {
    private Long pipelineId;
    private Map<String, Object> inputParams;
    private boolean async = false;
    private String triggerType; // manual, schedule, webhook
}
package com.example.datascreen.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SavePipelineRequest {

    private Long id;
    @NotBlank(message = "流程名称不能为空")
    private String name;

    private String description;

    @NotBlank(message = "definitionJson 不能为空")
    private String definitionJson;

    private String status;
}
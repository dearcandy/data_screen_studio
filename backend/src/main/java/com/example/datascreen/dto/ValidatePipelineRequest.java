package com.example.datascreen.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ValidatePipelineRequest {

    @NotBlank(message = "definitionJson 不能为空")
    private String definitionJson;

}
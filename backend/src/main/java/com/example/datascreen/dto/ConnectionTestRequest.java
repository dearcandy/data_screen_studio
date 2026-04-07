package com.example.datascreen.dto;

import com.example.datascreen.model.SourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 「一键测试连接」请求：在保存数据源前校验配置是否可用。
 */
@Data
@NoArgsConstructor
public class ConnectionTestRequest {

    /**
     * 数据源类型
     */
    @NotNull
    private SourceType type;

    /**
     * 数据源配置JSON
     */
    @NotBlank
    private String configJson;
}

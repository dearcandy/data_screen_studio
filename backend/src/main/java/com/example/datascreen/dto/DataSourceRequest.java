package com.example.datascreen.dto;

import com.example.datascreen.model.SourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建或更新「数据源」的请求体
 */
@Data
@NoArgsConstructor
public class DataSourceRequest {

    /**
     * 数据源名称
     */
    @NotBlank
    private String name;

    /**
     * 数据源类型
     */
    @NotNull
    private SourceType type;

    /**
     * 数据源连接信息
     */
    @NotBlank
    private String configJson;

    /**
     * 数据源备注
     */
    private String remark;
}

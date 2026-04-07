package com.example.datascreen.dto;

import com.example.datascreen.model.FetchMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建或更新「数据集」的请求体：定义取数方式、Mock、以及脚本加工逻辑。
 */
@Data
@NoArgsConstructor
public class DataSetRequest {

    /**
     * 数据集名称
     */
    @NotBlank
    private String name;

    /**
     * 数据源ID
     */
    private Long dataSourceId;

    /**
     * 数据获取模式 实时/Mock
     */
    @NotNull
    private FetchMode fetchMode;

    /**
     * 数据获取内容
     */
    private String fetchSpec;

    /**
     * mock的响应
     */
    private String mockJson;

    /**
     * 脚本内容
     */
    private String scriptText;

    /**
     * 是否有效
     */
    private boolean enabled;
}

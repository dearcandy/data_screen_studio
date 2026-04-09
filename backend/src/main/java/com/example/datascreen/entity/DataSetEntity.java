package com.example.datascreen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.datascreen.model.FetchMode;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * 数据集实体：描述如何取数、如何加工、如何对外发布。
 */
@Data
@NoArgsConstructor
@TableName("ds_data_set")
public class DataSetEntity {
    /**
     * 数据集ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 数据集名称
     */
    private String name;

    /**
     * 数据源ID
     */
    @TableField("data_source_id")
    private Long dataSourceId;

    /**
     * 数据获取模式
     */
    @TableField("fetch_mode")
    private FetchMode fetchMode = FetchMode.LIVE;

    /**
     * 数据获取配置
     */
    @TableField("fetch_spec")
    private String fetchSpec;

    /**
     * Mock数据内容
     */
    @TableField("mock_json")
    private String mockJson;

    /**
     * 数据处理脚本
     */
    @TableField("script_text")
    private String scriptText;

    /**
     * 数据集公开访问路径token
     */
    @TableField("public_token")
    private String publicToken = UUID.randomUUID().toString().replace("-", "");

    /**
     * 是否启用公开数据集访问
     */
    private boolean enabled = true;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private Instant createdAt = Instant.now();

    /**
     * 最后更新时间
     */
    @TableField("updated_at")
    private Instant updatedAt = Instant.now();
}

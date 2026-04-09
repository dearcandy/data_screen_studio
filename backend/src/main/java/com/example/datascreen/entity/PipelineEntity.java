package com.example.datascreen.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
@TableName("ds_pipeline")
public class PipelineEntity {

    /**
     * 流程ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 流程名称
     */
    private String name;

    /**
     * 流程描述
     */
    private String description;

    /**
     * 流程定义 JSON（完整 DAG 结构）
     * 也可将节点和连接拆分到子表，这里为了灵活先用 JSON
     */
    private String definitionJson;

    /**
     * 流程状态：draft / published / archived
     */
    private String status;

    /**
     * 创建人
     */
    private String creator;

    /**
     * 外部调用 token
     */
    @TableField("public_token")
    private String publicToken = UUID.randomUUID().toString().replace("-", "");

    /**
     * 是否开启外部调用
     */
    @TableField("external_enabled")
    private boolean externalEnabled = false;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;
}
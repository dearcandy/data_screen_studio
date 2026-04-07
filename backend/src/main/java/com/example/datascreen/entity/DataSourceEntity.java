package com.example.datascreen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.datascreen.model.SourceType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;


/**
 * 数据源实体：保存连接类型与配置 JSON，不保存实际业务数据。
 */
@Data
@NoArgsConstructor
@TableName("ds_data_source")
public class DataSourceEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 数据源展示名称
     */
    private String name;

    /**
     * 数据源类型
     */
    private SourceType type;

    /**
     * 数据源连接配置
     */
    @TableField("config_json")
    private String configJson;

    /**
     * 备注信息，便于区分环境用途。
     */
    private String remark;

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

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

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 数据集名称。 */
    private String name;

    /** Optional when FetchMode.MOCK */
    @TableField("data_source_id")
    private Long dataSourceId;

    /** 取数模式：LIVE 或 MOCK。 */
    @TableField("fetch_mode")
    private FetchMode fetchMode = FetchMode.LIVE;

    /** For JDBC: SQL; for Redis: key; for HTTP: path appended to base URL */
    @TableField("fetch_spec")
    private String fetchSpec;

    /** Mock payload as JSON (array or object) */
    @TableField("mock_json")
    private String mockJson;

    /**
     * JavaScript body: receives {@code input}, must return transformed value.
     * Example: {@code return input.filter(r => r.v > 0);}
     */
    @TableField("script_text")
    private String scriptText;

    @TableField("public_token")
    private String publicToken = UUID.randomUUID().toString().replace("-", "");

    /** 是否启用嵌入接口访问。 */
    private boolean enabled = true;

    /** 创建时间。 */
    @TableField("created_at")
    private Instant createdAt = Instant.now();

    /** 最后更新时间。 */
    @TableField("updated_at")
    private Instant updatedAt = Instant.now();
}

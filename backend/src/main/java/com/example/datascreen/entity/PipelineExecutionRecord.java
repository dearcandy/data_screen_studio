package com.example.datascreen.entity;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.*;
import java.time.Instant;

@Data
@TableName("ds_pipeline_execution")
public class PipelineExecutionRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long pipelineId;
    /**
     * 全局唯一，用于查询结果
     */
    private String executionId;

    /**
     * 触发方式：manual / schedule / webhook
     */
    private String triggerType;

    /**
     * 输入参数（JSON）
     */
    private String inputParams;

    /**
     * 执行状态：pending / running / success / failed / cancelled
     */
    private String status;

    /**
     * 最终输出结果（JSON 或 存储路径）
     */
    private String outputResult;

    /**
     * 错误信息
     */
    private String errorMsg;

    private Instant startTime;
    private Instant endTime;

    /**
     * 各节点执行明细（可用 JSON 存储或另建明细表）
     */
    private String nodeDetailsJson;
}
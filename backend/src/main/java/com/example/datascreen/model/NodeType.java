package com.example.datascreen.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Pipeline 节点类型枚举
 */
@Getter
public enum NodeType {

    DATA_SOURCE("dataSource", "数据源", "提供数据库或接口的连接配置"),
    FETCH("fetch", "即时取数", "直接基于数据源配置进行一次抓取"),
    DATA_SET("dataSet", "数据集", "从数据源获取数据，可进行预处理"),
    SCRIPT("script", "脚本", "使用 Groovy/Python 等脚本处理数据"),
    CONDITION("condition", "条件分支", "根据表达式选择下游路径"),
    PARALLEL("parallel", "并行", "并发执行多个子节点，并合并结果"),
    OUTPUT("output", "输出", "输出最终结果，可格式化"),
    START("start", "开始", "Pipeline 起始节点（隐式）"),
    END("end", "结束", "Pipeline 结束节点（隐式）");

    private final String code;
    private final String name;
    private final String description;

    NodeType(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    /**
     * 根据代码字符串获取枚举
     */
    public static NodeType fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("未知的节点类型编码: " + code);
        }
        for (NodeType type : values()) {
            if (type.code.equals(code) || type.name().equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的节点类型编码: " + code);
    }

    @JsonCreator
    public static NodeType fromJson(String value) {
        return fromCode(value);
    }

    @JsonValue
    public String toJson() {
        return this.code;
    }

    /**
     * 判断是否为数据源相关节点
     */
    public boolean isDataSourceRelated() {
        return this == DATA_SOURCE || this == DATA_SET || this == FETCH;
    }

    /**
     * 判断是否为控制流节点
     */
    public boolean isControlNode() {
        return this == CONDITION || this == PARALLEL;
    }
}
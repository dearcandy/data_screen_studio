package com.example.datascreen.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * Pipeline 节点定义（运行时模型）
 */
@Data
@NoArgsConstructor
public class Node {

    /**
     * 节点唯一标识（在 Pipeline 内唯一）
     */
    private String id;

    /**
     * 节点类型：dataSource / dataSet / script / condition / parallel / output
     */
    private NodeType type;

    /**
     * 节点展示名称（可选）
     */
    private String name;

    /**
     * 依赖的前置节点 ID 列表
     */
    private List<String> dependsOn;

    /**
     * 节点特有配置（根据 type 不同，结构不同）
     * 例如：
     *   dataSet 类型：{ "dataSetId": 123, "fetchSql": "select * from ..." }
     *   script 类型：{ "language": "groovy", "source": "return input;" }
     *   condition 类型：{ "expression": "input > 0", "trueBranch": "nodeA", "falseBranch": "nodeB" }
     *   parallel 类型：{ "branches": [...], "mergeStrategy": "mergeToList" }
     */
    private Map<String, Object> config;

    /**
     * 超时时间（毫秒），可选
     */
    private Long timeoutMs;

    /**
     * 重试次数，默认 0
     */
    private Integer retryCount = 0;

    /**
     * 是否忽略失败继续执行（仅对非关键节点有效）
     */
    private Boolean ignoreFailure = false;

    /**
     * 辅助方法：获取配置中的字符串值
     */
    public String getConfigString(String key) {
        if (config == null) {
            return null;
        }
        Object value = config.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 获取配置中的整数值
     */
    public Integer getConfigInteger(String key) {
        if (config == null) {
            return null;
        }
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    /**
     * 获取配置中的整数值
     */
    public Long getConfigLong(String key) {
        if (config == null) {
            return null;
        }
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    /**
     * 获取配置中的列表值
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getConfigList(String key) {
        if (config == null) {
            return null;
        }
        Object value = config.get(key);
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        return null;
    }

    /**
     * 获取配置中的 Map 值
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getConfigMap(String key) {
        if (config == null) {
            return null;
        }
        Object value = config.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }
}
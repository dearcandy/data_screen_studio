package com.example.datascreen.service.pipeline;

import lombok.Data;

import java.util.Map;

/**
 * 脚本上下文：用于存储脚本执行时的输入、参数和全局变量。
 */
@Data
public class ScriptContext {
    /**
     * 脚本输入数据
     */
    private Object input;
    /**
     * 脚本输出数据
     */
    private Object output;
    /**
     * 脚本参数映射
     */
    private Map<String, Object> params;
    /**
     * 全局变量映射
     */
    private Map<String, Object> globalContext;
}


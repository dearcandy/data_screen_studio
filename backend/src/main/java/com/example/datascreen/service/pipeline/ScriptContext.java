package com.example.datascreen.service.pipeline;

import lombok.Data;

import java.util.Map;

// ScriptContext.java
@Data
public class ScriptContext {
    private Object input;
    private Map<String, Object> params;
    private Map<String, Object> globalContext; // 全局变量引用
}


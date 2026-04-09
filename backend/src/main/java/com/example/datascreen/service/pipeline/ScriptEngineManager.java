package com.example.datascreen.service.pipeline;

import com.example.datascreen.service.ScriptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 脚本执行统一入口：当前仅接入现有 GraalJS {@link ScriptService}。
 */
@Component
@RequiredArgsConstructor
public class ScriptEngineManager {
    private final ScriptService scriptService;

    public Object execute(String language, String source, ScriptContext ctx) {
        if (source == null || source.isBlank()) {
            return ctx != null ? ctx.getInput() : null;
        }
        // 先兼容前端 language 字段，统一走 JS
        try {
            Object input = ctx != null ? ctx.getInput() : null;
            return scriptService.run(input, source);
        } catch (Exception e) {
            throw new RuntimeException("脚本执行失败", e);
        }
    }
}
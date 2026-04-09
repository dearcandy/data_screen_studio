package com.example.datascreen.service.pipeline;

import com.example.datascreen.service.ScriptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 脚本引擎管理器：负责调用 {@link ScriptService} 执行脚本。
 */
@Component
@RequiredArgsConstructor
public class ScriptEngineManager {

    /**
     * 脚本服务实例
     */
    private final ScriptService scriptService;

    /**
     * 执行脚本
     * @param language 脚本语言（当前仅支持 JS）
     * @param source 脚本源代码
     * @param ctx 脚本上下文
     * @return 脚本执行结果
     * @throws RuntimeException 执行异常
     */
    public Object execute(String language, String source, ScriptContext ctx) {
        if (source == null || source.isBlank()) {
            return ctx != null ? ctx.getInput() : null;
        }
        // 先兼容前端 language 字段，统一走 JS 执行
        if (!"javascript".equals(language)) {
            throw new IllegalArgumentException("仅支持 JS 脚本");
        }
        try {
            Object input = ctx != null ? ctx.getInput() : null;
            return scriptService.run(input, source);
        } catch (Exception e) {
            throw new RuntimeException("脚本执行失败", e);
        }
    }
}
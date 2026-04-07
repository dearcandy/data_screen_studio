package com.example.datascreen.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.springframework.stereotype.Service;

/**
 * 脚本执行服务：使用 GraalJS 对数据做过滤、聚合、结构转换。
 */
@Service
public class ScriptService {

    private final ObjectMapper objectMapper;

    public ScriptService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 执行脚本；无脚本时原样返回输入。 */
    public Object run(Object input, String scriptText) throws Exception {
        if (scriptText == null || scriptText.isBlank()) {
            return input;
        }
        try (Context ctx = Context.newBuilder("js")
                .allowExperimentalOptions(true)
                .option("js.ecmascript-version", "2022")
                .option("engine.WarnInterpreterOnly", "false")
                .build()) {
            String json = objectMapper.writeValueAsString(input);
            String wrapped = "(function() {\n"
                    + "const input = "
                    + json
                    + ";\n"
                    + scriptText
                    + "\n})()";
            Value out = ctx.eval("js", wrapped);
            return valueToJava(out);
        } catch (PolyglotException e) {
            throw new IllegalArgumentException("脚本执行失败: " + e.getMessage(), e);
        }
    }

    /** 将 Graal Value 递归转换为 Java 基础结构。 */
    private Object valueToJava(Value out) throws Exception {
        if (out.isNull()) {
            return null;
        }
        if (out.isString()) {
            return out.asString();
        }
        if (out.isBoolean()) {
            return out.asBoolean();
        }
        if (out.isNumber()) {
            if (out.fitsInInt()) {
                return out.asInt();
            }
            if (out.fitsInLong()) {
                return out.asLong();
            }
            return out.asDouble();
        }
        if (out.hasArrayElements()) {
            long n = out.getArraySize();
            java.util.List<Object> list = new java.util.ArrayList<>();
            for (long i = 0; i < n; i++) {
                list.add(valueToJava(out.getArrayElement(i)));
            }
            return list;
        }
        if (out.hasMembers()) {
            java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
            for (String key : out.getMemberKeys()) {
                map.put(key, valueToJava(out.getMember(key)));
            }
            return map;
        }
        return objectMapper.readValue(out.toString(), Object.class);
    }
}

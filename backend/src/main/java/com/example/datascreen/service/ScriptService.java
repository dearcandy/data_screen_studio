package com.example.datascreen.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.springframework.stereotype.Service;

/**
 * 脚本执行服务：负责执行用户自定义的脚本，如数据过滤、聚合、结构转换等。
 */
@Service
@RequiredArgsConstructor
public class ScriptService {

    private final ObjectMapper objectMapper;


    /**
     * 执行用户自定义的脚本，返回执行结果。
     * @param input 输入数据
     * @param scriptText 脚本文本内容
     * @return 执行结果，根据脚本内容返回不同类型的数据
     */
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

    /**
     * 将 Graal Value 递归转换为 Java 基础结构。
     * @param out 要转换的 Graal Value
     * @return 转换后的 Java 基础结构
     * @throws Exception 如果转换过程中发生错误
     */
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

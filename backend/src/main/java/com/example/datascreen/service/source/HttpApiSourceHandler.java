package com.example.datascreen.service.source;

import com.example.datascreen.constant.HttpConstants;
import com.example.datascreen.model.SourceType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * HTTP API 数据源处理器
 */
@Component
public class HttpApiSourceHandler implements DataSourceTypeHandler {

    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public HttpApiSourceHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(HttpConstants.WEB_CLIENT_MAX_IN_MEMORY_BYTES))
                .build();
    }

    @Override
    public SourceType supportType() {
        return SourceType.HTTP_API;
    }

    @Override
    public void validateConfig(JsonNode cfg) {
        HandlerValidationSupport.checkUnknown(cfg, Set.of("baseUrl", "testPath", "method", "params", "headers", "body"));
        HandlerValidationSupport.require(cfg, "baseUrl");

        if (cfg.has("method")) {
            String method = cfg.get("method").asText("").trim().toUpperCase(Locale.ROOT);
            if (!HttpConstants.ALLOWED_METHODS.contains(method)) {
                throw new IllegalArgumentException("method 仅支持 GET / POST");
            }
        }
        if (cfg.has("params") && !cfg.get("params").isObject()) {
            throw new IllegalArgumentException("params 必须是 JSON 对象");
        }
        if (cfg.has("headers") && !cfg.get("headers").isObject()) {
            throw new IllegalArgumentException("headers 必须是 JSON 对象");
        }
        // body 可以是任意类型，不做强校验
    }

    @Override
    public void testConnection(JsonNode cfg) {
        String baseUrl = extractBaseUrl(cfg);
        String testPath = cfg.path("testPath").asText(HttpConstants.DEFAULT_PATH);
        String method = extractMethod(cfg, null); // 优先使用数据源配置的 method
        JsonNode params = cfg.has("params") ? cfg.get("params") : null;
        JsonNode headers = cfg.has("headers") ? cfg.get("headers") : null;
        JsonNode body = cfg.has("body") ? cfg.get("body") : null;

        String url = buildUrl(baseUrl, testPath, params);
        HttpHeaders httpHeaders = buildHeaders(headers);

        RequestBodySpec request = webClient.method(HttpMethod.valueOf(method))
                .uri(url)
                .headers(h -> h.addAll(httpHeaders));

        if (HttpConstants.METHOD_POST.equals(method) && body != null && !body.isNull()) {
            request.bodyValue(body);
        }

        request.retrieve()
                .toBodilessEntity()
                .timeout(HttpConstants.TEST_TIMEOUT)
                .block();
    }

    @Override
    public Object fetch(JsonNode cfg, String fetchSpec) throws Exception {
        String baseUrl = extractBaseUrl(cfg);
        HttpRequestSpec spec = resolveHttpSpec(cfg, fetchSpec);

        String url = buildUrl(baseUrl, spec.path(), spec.params());
        HttpHeaders headers = buildHeaders(spec.headers());

        RequestBodySpec request = webClient.method(HttpMethod.valueOf(spec.method()))
                .uri(url)
                .headers(h -> h.addAll(headers));

        if (HttpConstants.METHOD_POST.equals(spec.method()) && spec.body() != null && !spec.body().isNull()) {
            // 将 JsonNode 转换为普通对象发送（保持原始结构）
            request.bodyValue(objectMapper.treeToValue(spec.body(), Object.class));
        }

        String responseBody = request.retrieve()
                .bodyToMono(String.class)
                .timeout(HttpConstants.FETCH_TIMEOUT)
                .block();

        if (responseBody == null || responseBody.isBlank()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readValue(responseBody, Object.class);
    }

    // ========== 私有辅助方法 ==========

    private String extractBaseUrl(JsonNode cfg) {
        String base = cfg.path("baseUrl").asText("");
        if (base.isBlank()) {
            throw new IllegalArgumentException("HTTP 配置缺少 baseUrl");
        }
        return base;
    }

    private String extractMethod(JsonNode cfg, JsonNode specNode) {
        String method = HttpConstants.METHOD_GET;
        if (specNode != null && specNode.has("method")) {
            method = specNode.path("method").asText(HttpConstants.METHOD_GET);
        } else if (cfg.has("method")) {
            method = cfg.path("method").asText(HttpConstants.METHOD_GET);
        }
        method = method.trim().toUpperCase(Locale.ROOT);
        if (!HttpConstants.ALLOWED_METHODS.contains(method)) {
            throw new IllegalArgumentException("HTTP method 仅支持 GET / POST");
        }
        return method;
    }

    private HttpHeaders buildHeaders(JsonNode headersNode) {
        HttpHeaders headers = new HttpHeaders();
        if (headersNode != null && headersNode.isObject()) {
            headersNode.fields().forEachRemaining(e -> headers.add(e.getKey(), e.getValue().asText()));
        }
        return headers;
    }

    private JsonNode pickJsonNode(JsonNode specNode, JsonNode cfg, String key) {
        JsonNode value = null;
        if (specNode != null && specNode.has(key)) {
            value = specNode.get(key);
        } else if (cfg.has(key)) {
            value = cfg.get(key);
        }
        if (value != null && !value.isObject()) {
            throw new IllegalArgumentException("HTTP " + key + " 必须是 JSON 对象");
        }
        return value;
    }

    private HttpRequestSpec resolveHttpSpec(JsonNode cfg, String fetchSpec) throws Exception {
        JsonNode specNode = parseFetchSpec(fetchSpec);
        String path = HttpConstants.DEFAULT_PATH;
        if (specNode != null && specNode.has("path")) {
            path = specNode.path("path").asText(HttpConstants.DEFAULT_PATH);
        } else if (fetchSpec != null && !fetchSpec.isBlank()) {
            path = fetchSpec.trim();
        }
        String method = extractMethod(cfg, specNode);
        JsonNode params = pickJsonNode(specNode, cfg, "params");
        JsonNode headers = pickJsonNode(specNode, cfg, "headers");
        JsonNode body = (specNode != null && specNode.has("body")) ? specNode.get("body") : cfg.get("body");
        return new HttpRequestSpec(path, method, params, headers, body);
    }

    private JsonNode parseFetchSpec(String fetchSpec) throws Exception {
        if (fetchSpec == null || fetchSpec.isBlank()) {
            return null;
        }
        String trimmed = fetchSpec.trim();
        if (!trimmed.startsWith("{")) {
            return null;
        }
        JsonNode node = objectMapper.readTree(trimmed);
        if (!node.isObject()) {
            throw new IllegalArgumentException("HTTP fetchSpec JSON 必须是对象");
        }
        return node;
    }

    /**
     * 拼接 baseUrl 和 path，智能处理斜杠
     */
    private static String joinUrl(String base, String path) {
        if (path == null || path.isEmpty()) {
            path = HttpConstants.DEFAULT_PATH;
        }
        boolean baseEndsWithSlash = base.endsWith("/");
        boolean pathStartsWithSlash = path.startsWith("/");
        if (baseEndsWithSlash && pathStartsWithSlash) {
            return base.substring(0, base.length() - 1) + path;
        }
        if (!baseEndsWithSlash && !pathStartsWithSlash) {
            return base + "/" + path;
        }
        return base + path;
    }

    /**
     * 构建完整 URL，添加 query 参数
     */
    private static String buildUrl(String base, String path, JsonNode params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(joinUrl(base, path));
        if (params != null && params.isObject()) {
            for (Iterator<Map.Entry<String, JsonNode>> it = params.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> entry = it.next();
                JsonNode value = entry.getValue();
                if (value == null || value.isNull()) {
                    continue;
                }
                if (value.isArray()) {
                    for (JsonNode item : value) {
                        if (item != null && !item.isNull()) {
                            builder.queryParam(entry.getKey(), item.asText());
                        }
                    }
                } else {
                    builder.queryParam(entry.getKey(), value.asText());
                }
            }
        }
        return builder.build(true).toUriString();
    }

    private record HttpRequestSpec(String path, String method, JsonNode params, JsonNode headers, JsonNode body) {
    }
}
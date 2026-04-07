package com.example.datascreen.service.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.datascreen.model.SourceType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class HttpApiSourceHandler implements DataSourceTypeHandler {
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public HttpApiSourceHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(32 * 1024 * 1024))
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
            String method = cfg.get("method").asText("").trim();
            if (!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
                throw new IllegalArgumentException("method 仅支持 GET / POST");
            }
        }
        if (cfg.has("params") && !cfg.get("params").isObject()) {
            throw new IllegalArgumentException("params 必须是 JSON 对象");
        }
        if (cfg.has("headers") && !cfg.get("headers").isObject()) {
            throw new IllegalArgumentException("headers 必须是 JSON 对象");
        }
    }

    @Override
    public void testConnection(JsonNode cfg) {
        String base = cfg.path("baseUrl").asText("");
        if (base.isBlank()) {
            throw new IllegalArgumentException("HTTP 配置缺少 baseUrl");
        }
        String path = cfg.path("testPath").asText("/");
        String method = cfg.path("method").asText("GET").trim().toUpperCase(Locale.ROOT);
        if (!"GET".equals(method) && !"POST".equals(method)) {
            throw new IllegalArgumentException("HTTP method 仅支持 GET / POST");
        }
        String url = buildUrl(base, path, cfg.get("params"));
        HttpHeaders headers = new HttpHeaders();
        if (cfg.has("headers") && cfg.get("headers").isObject()) {
            cfg.get("headers").fields().forEachRemaining(e -> headers.add(e.getKey(), e.getValue().asText()));
        }
        RequestBodySpec req = webClient.method(HttpMethod.valueOf(method)).uri(url).headers(h -> h.addAll(headers));
        if ("POST".equals(method) && cfg.has("body")) {
            req.bodyValue(cfg.get("body"));
        }
        req.retrieve().toBodilessEntity().timeout(Duration.ofSeconds(15)).block();
    }

    @Override
    public Object fetch(JsonNode cfg, String fetchSpec) throws Exception {
        String base = cfg.path("baseUrl").asText("");
        if (base.isBlank()) {
            throw new IllegalArgumentException("HTTP 配置缺少 baseUrl");
        }
        HttpRequestSpec spec = resolveHttpSpec(cfg, fetchSpec);
        String url = buildUrl(base, spec.path(), spec.params());
        HttpHeaders headers = new HttpHeaders();
        if (spec.headers() != null && spec.headers().isObject()) {
            spec.headers().fields().forEachRemaining(e -> headers.add(e.getKey(), e.getValue().asText()));
        }
        RequestBodySpec req = webClient.method(HttpMethod.valueOf(spec.method())).uri(url).headers(h -> h.addAll(headers));
        if ("POST".equals(spec.method()) && spec.body() != null && !spec.body().isNull()) {
            req.bodyValue(objectMapper.treeToValue(spec.body(), Object.class));
        }
        String body = req.retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .block();
        if (body == null || body.isBlank()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readValue(body, Object.class);
    }

    private HttpRequestSpec resolveHttpSpec(JsonNode cfg, String fetchSpec) throws Exception {
        JsonNode specNode = parseFetchSpec(fetchSpec);
        String path = "/";
        if (specNode != null && specNode.has("path")) {
            path = specNode.path("path").asText("/");
        } else if (fetchSpec != null && !fetchSpec.isBlank()) {
            path = fetchSpec.trim();
        }
        String method = readHttpMethod(specNode, cfg);
        JsonNode params = pickObject(specNode, cfg, "params");
        JsonNode headers = pickObject(specNode, cfg, "headers");
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

    private String readHttpMethod(JsonNode specNode, JsonNode cfg) {
        String method = "GET";
        if (specNode != null && specNode.has("method")) {
            method = specNode.path("method").asText("GET");
        } else if (cfg.has("method")) {
            method = cfg.path("method").asText("GET");
        }
        method = method.trim().toUpperCase(Locale.ROOT);
        if (!"GET".equals(method) && !"POST".equals(method)) {
            throw new IllegalArgumentException("HTTP method 仅支持 GET / POST");
        }
        return method;
    }

    private JsonNode pickObject(JsonNode specNode, JsonNode cfg, String key) {
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

    private static String joinUrl(String base, String path) {
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        if (!base.endsWith("/") && !path.startsWith("/")) {
            return base + "/" + path;
        }
        if (base.endsWith("/") && path.startsWith("/")) {
            return base.substring(0, base.length() - 1) + path;
        }
        return base + path;
    }

    private static String buildUrl(String base, String path, JsonNode params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(joinUrl(base, path));
        if (params != null && params.isObject()) {
            for (Map.Entry<String, JsonNode> e : iterable(params.fields())) {
                JsonNode value = e.getValue();
                if (value == null || value.isNull()) {
                    continue;
                }
                if (value.isArray()) {
                    for (JsonNode item : value) {
                        if (item != null && !item.isNull()) {
                            builder.queryParam(e.getKey(), item.asText());
                        }
                    }
                } else {
                    builder.queryParam(e.getKey(), value.asText());
                }
            }
        }
        return builder.build(true).toUriString();
    }

    private static <T> Iterable<T> iterable(java.util.Iterator<T> iterator) {
        return () -> iterator;
    }

    private record HttpRequestSpec(String path, String method, JsonNode params, JsonNode headers, JsonNode body) {
    }
}

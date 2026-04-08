package com.example.datascreen.service.source;

import com.example.datascreen.model.SourceType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

/**
 * Redis 数据源处理器：支持连接测试和键值读取。
 */
@Component
public class RedisSourceHandler implements DataSourceTypeHandler {

    private static final Duration TEST_CONNECTION_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration FETCH_TIMEOUT = Duration.ofSeconds(5);
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 6379;
    private static final int DEFAULT_DATABASE = 0;

    private final ObjectMapper objectMapper;

    public RedisSourceHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public SourceType supportType() {
        return SourceType.REDIS;
    }

    @Override
    public void validateConfig(JsonNode cfg) {
        HandlerValidationSupport.checkUnknown(cfg, Set.of("host", "port", "password", "database"));
        HandlerValidationSupport.require(cfg, "host", "port");
        // 校验 port 为正整数
        HandlerValidationSupport.requirePositiveInt(cfg);
        if (cfg.has("database")) {
            // 校验 database >= 0
            HandlerValidationSupport.requireNonNegativeInt(cfg);
        }
        // password 可选，无额外校验
    }

    @Override
    public void testConnection(JsonNode cfg) {
        RedisURI uri = buildUri(cfg, TEST_CONNECTION_TIMEOUT);
        RedisClient client = null;
        try {
            client = RedisClient.create(uri);
            try (StatefulRedisConnection<String, String> conn = client.connect()) {
                conn.sync().ping();
            }
        } finally {
            if (client != null) {
                client.shutdown();
            }
        }
    }

    @Override
    public Object fetch(JsonNode cfg, String fetchSpec) throws Exception {
        String key = (fetchSpec != null) ? fetchSpec.trim() : "";
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Redis 请在数据集 fetchSpec 中填写 key");
        }

        RedisURI uri = buildUri(cfg, FETCH_TIMEOUT);
        RedisClient client = null;
        try {
            client = RedisClient.create(uri);
            try (StatefulRedisConnection<String, String> conn = client.connect()) {
                String value = conn.sync().get(key);
                if (value == null) {
                    return objectMapper.createObjectNode(); // 空对象
                }
                // 尝试解析 JSON，失败则返回原始字符串
                try {
                    return objectMapper.readValue(value, Object.class);
                } catch (Exception e) {
                    return value;
                }
            }
        } finally {
            if (client != null) {
                client.shutdown();
            }
        }
    }

    /**
     * 根据配置构建 RedisURI
     *
     * @param cfg     数据源配置节点
     * @param timeout 连接超时和命令超时
     * @return RedisURI 实例
     */
    private RedisURI buildUri(JsonNode cfg, Duration timeout) {
        String host = cfg.path("host").asText(DEFAULT_HOST);
        int port = cfg.path("port").asInt(DEFAULT_PORT);
        String password = cfg.path("password").asText(null);
        int database = cfg.path("database").asInt(DEFAULT_DATABASE);

        RedisURI.Builder builder = RedisURI.builder()
                .withHost(host)
                .withPort(port)
                .withDatabase(database)
                .withTimeout(timeout);

        if (password != null && !password.isEmpty()) {
            builder.withPassword(password.toCharArray());
        }
        return builder.build();
    }
}
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

@Component
public class RedisSourceHandler implements DataSourceTypeHandler {
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
        HandlerValidationSupport.requirePositiveInt(cfg);
        if (cfg.has("database")) {
            HandlerValidationSupport.requireNonNegativeInt(cfg);
        }
    }

    /**
     * 测试Redis数据源连接
     * @param cfg 连接配置
     */
    @Override
    public void testConnection(JsonNode cfg) {
        RedisClient client = RedisClient.create(buildUri(cfg, Duration.ofSeconds(3)));
        try (StatefulRedisConnection<String, String> conn = client.connect()) {
            conn.sync().ping();
        } finally {
            client.shutdown();
        }
    }

    /**
     * 获取Redis数据
     * @param cfg 连接配置
     * @param fetchSpec 拉取设置
     * @return 数据对象
     */
    @Override
    public Object fetch(JsonNode cfg, String fetchSpec){
        String key = fetchSpec != null ? fetchSpec : "";
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Redis 请在数据集 fetchSpec 中填写 key");
        }
        RedisClient client = RedisClient.create(buildUri(cfg, Duration.ofSeconds(5)));
        try (StatefulRedisConnection<String, String> conn = client.connect()) {
            String val = conn.sync().get(key);
            if (val == null) {
                return objectMapper.createObjectNode();
            }
            try {
                return objectMapper.readValue(val, Object.class);
            } catch (Exception e) {
                return val;
            }
        } finally {
            client.shutdown();
        }
    }

    /**
     * 解析配置 构造RedisURI
     * @param cfg 配置
     * @param timeout 超时时间
     * @return RedisURI
     */
    private RedisURI buildUri(JsonNode cfg, Duration timeout) {
        String host = cfg.path("host").asText("127.0.0.1");
        int port = cfg.path("port").asInt(6379);
        String password = cfg.path("password").asText(null);
        int db = cfg.path("database").asInt(0);
        RedisURI.Builder b = RedisURI.builder().withHost(host).withPort(port).withDatabase(db).withTimeout(timeout);
        if (password != null && !password.isEmpty()) {
            b.withPassword(password.toCharArray());
        }
        return b.build();
    }
}

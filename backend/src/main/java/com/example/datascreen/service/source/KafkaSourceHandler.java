package com.example.datascreen.service.source;

import com.example.datascreen.constant.KafkaConstants;
import com.example.datascreen.model.SourceType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Kafka 数据源处理器：测试连接使用 AdminClient，取数使用短生命周期 Consumer。
 */
@Component
public class KafkaSourceHandler implements DataSourceTypeHandler {

    private final ObjectMapper objectMapper;

    public KafkaSourceHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public SourceType supportType() {
        return SourceType.KAFKA;
    }

    @Override
    public void validateConfig(JsonNode cfg) {
        HandlerValidationSupport.checkUnknown(cfg, Set.of(
                "bootstrapServers",
                "testTopic",
                "securityProtocol",
                "saslMechanism",
                "saslUsername",
                "saslPassword",
                "defaultTopic",
                "defaultMaxRecords",
                "defaultPollTimeoutMs",
                "defaultAutoOffsetReset"
        ));
        HandlerValidationSupport.require(cfg, "bootstrapServers");

        // 校验 securityProtocol
        if (cfg.has("securityProtocol")) {
            String sp = cfg.path("securityProtocol").asText("").trim();
            if (!KafkaConstants.ALLOWED_SECURITY_PROTOCOLS.contains(sp)) {
                throw new IllegalArgumentException("securityProtocol 必须是 PLAINTEXT / SSL / SASL_PLAINTEXT / SASL_SSL 之一");
            }
        }

        // 校验数值参数
        if (cfg.has("defaultMaxRecords")) {
            HandlerValidationSupport.requirePositiveInt(cfg, "defaultMaxRecords");
        }
        if (cfg.has("defaultPollTimeoutMs")) {
            HandlerValidationSupport.requirePositiveInt(cfg, "defaultPollTimeoutMs");
        }

        // 校验 offset reset
        if (cfg.has("defaultAutoOffsetReset")) {
            String v = cfg.path("defaultAutoOffsetReset").asText("").trim().toLowerCase();
            if (!KafkaConstants.ALLOWED_AUTO_OFFSET_RESET.contains(v)) {
                throw new IllegalArgumentException("defaultAutoOffsetReset 仅支持 latest / earliest");
            }
        }

        // SASL 校验
        String user = cfg.path("saslUsername").asText("").trim();
        String mech = cfg.path("saslMechanism").asText("").trim();
        String pass = cfg.path("saslPassword").asText("");
        if (!user.isEmpty() || !mech.isEmpty()) {
            if (user.isEmpty() || mech.isEmpty()) {
                throw new IllegalArgumentException("SASL 需同时填写 saslUsername 与 saslMechanism");
            }
            if (pass.isEmpty()) {
                throw new IllegalArgumentException("SASL 需填写 saslPassword");
            }
            if (!KafkaConstants.SUPPORTED_SASL_MECHANISMS.contains(mech)) {
                throw new IllegalArgumentException("saslMechanism 仅支持 PLAIN、SCRAM-SHA-256、SCRAM-SHA-512");
            }
            String sp = cfg.path("securityProtocol").asText(KafkaConstants.SECURITY_PLAINTEXT).trim();
            if (!KafkaConstants.SASL_PLAINTEXT.equals(sp) && !KafkaConstants.SASL_SSL.equals(sp)) {
                throw new IllegalArgumentException("使用 SASL 时请将 securityProtocol 设为 SASL_PLAINTEXT 或 SASL_SSL");
            }
        }
    }

    @Override
    public void testConnection(JsonNode cfg) throws Exception {
        Properties props = buildClientProperties(cfg, KafkaConstants.CLIENT_ID_PREFIX_TEST + UUID.randomUUID());
        try (AdminClient admin = AdminClient.create(props)) {
            DescribeClusterOptions clusterOpts = new DescribeClusterOptions()
                    .timeoutMs(KafkaConstants.REQUEST_TIMEOUT_MS);
            admin.describeCluster(clusterOpts)
                    .clusterId()
                    .get(KafkaConstants.ADMIN_OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            String testTopic = cfg.path("testTopic").asText("").trim();
            if (!testTopic.isEmpty()) {
                admin.describeTopics(Collections.singleton(testTopic))
                        .all()
                        .get(KafkaConstants.ADMIN_OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
        }
    }

    @Override
    public Object fetch(JsonNode cfg, String fetchSpec) throws Exception {
        KafkaFetchParams params = resolveFetchParams(cfg, fetchSpec);
        Properties props = buildClientProperties(cfg, KafkaConstants.CLIENT_ID_PREFIX_FETCH + UUID.randomUUID());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, KafkaConstants.CLIENT_ID_PREFIX_FETCH + UUID.randomUUID());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, params.autoOffsetReset());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG,
                String.valueOf(Math.min(params.maxRecords(), KafkaConstants.MAX_POLL_RECORDS_CAP)));

        List<Map<String, Object>> result = new ArrayList<>();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(params.topic()));
            // 触发分区分配
            consumer.poll(Duration.ofMillis(KafkaConstants.CONSUMER_INITIAL_POLL_MS));

            long deadline = System.nanoTime() + Duration.ofMillis(params.pollTimeoutMs()).toNanos();
            result = pollRecords(consumer, params.maxRecords(), deadline);
        }
        return result;
    }

    /**
     * 轮询 Kafka 拉取消息，直到达到数量上限或超时。
     */
    private List<Map<String, Object>> pollRecords(KafkaConsumer<String, String> consumer,
                                                  int maxRecords,
                                                  long deadlineNanos) throws Exception {
        List<Map<String, Object>> records = new ArrayList<>();
        while (records.size() < maxRecords && System.nanoTime() < deadlineNanos) {
            long remainingMs = TimeUnit.NANOSECONDS.toMillis(deadlineNanos - System.nanoTime());
            long pollTimeout = Math.min(Math.max(remainingMs, 10), KafkaConstants.CONSUMER_POLL_SLICE_MS);
            ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(pollTimeout));
            for (ConsumerRecord<String, String> r : consumerRecords) {
                if (records.size() >= maxRecords) {
                    break;
                }
                records.add(recordToMap(r));
            }
        }
        return records;
    }

    private Map<String, Object> recordToMap(ConsumerRecord<String, String> r) throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("topic", r.topic());
        map.put("partition", r.partition());
        map.put("offset", r.offset());
        if (r.timestampType() != org.apache.kafka.common.record.TimestampType.NO_TIMESTAMP_TYPE) {
            map.put("timestamp", r.timestamp());
        }
        map.put("key", r.key());

        String value = r.value();
        if (value == null || value.isBlank()) {
            map.put("value", null);
        } else {
            String trimmed = value.trim();
            if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                    (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
                try {
                    map.put("value", objectMapper.readValue(value, Object.class));
                } catch (Exception e) {
                    map.put("value", value);
                }
            } else {
                map.put("value", value);
            }
        }
        return map;
    }

    private KafkaFetchParams resolveFetchParams(JsonNode cfg, String fetchSpec) {
        String defaultTopic = cfg.path("defaultTopic").asText("").trim();
        int defaultMax = cfg.path("defaultMaxRecords").asInt(KafkaConstants.DEFAULT_MAX_RECORDS);
        int defaultPoll = cfg.path("defaultPollTimeoutMs").asInt(KafkaConstants.DEFAULT_POLL_TIMEOUT_MS);
        String defaultReset = cfg.path("defaultAutoOffsetReset").asText(KafkaConstants.AUTO_OFFSET_LATEST).trim().toLowerCase();
        if (!KafkaConstants.ALLOWED_AUTO_OFFSET_RESET.contains(defaultReset)) {
            defaultReset = KafkaConstants.AUTO_OFFSET_LATEST;
        }

        if (fetchSpec == null || fetchSpec.isBlank()) {
            if (defaultTopic.isEmpty()) {
                throw new IllegalArgumentException("Kafka 请在数据集 fetchSpec 填写 topic，或在数据源配置 defaultTopic");
            }
            return new KafkaFetchParams(defaultTopic, defaultMax, defaultPoll, defaultReset);
        }

        String raw = fetchSpec.trim();
        if (raw.startsWith("{")) {
            try {
                JsonNode node = objectMapper.readTree(raw);
                if (!node.isObject()) {
                    throw new IllegalArgumentException("Kafka fetchSpec JSON 必须是对象");
                }
                String topic = node.path("topic").asText("").trim();
                if (topic.isEmpty()) {
                    topic = defaultTopic;
                }
                if (topic.isEmpty()) {
                    throw new IllegalArgumentException("Kafka fetchSpec 缺少 topic，且数据源未配置 defaultTopic");
                }

                int max = node.has("maxRecords") ? node.path("maxRecords").asInt(defaultMax) : defaultMax;
                int poll = node.has("pollTimeoutMs") ? node.path("pollTimeoutMs").asInt(defaultPoll) : defaultPoll;
                String reset = node.has("autoOffsetReset")
                        ? node.path("autoOffsetReset").asText(defaultReset).trim().toLowerCase()
                        : defaultReset;

                if (!KafkaConstants.ALLOWED_AUTO_OFFSET_RESET.contains(reset)) {
                    throw new IllegalArgumentException("autoOffsetReset 仅支持 latest / earliest");
                }
                if (max <= 0 || poll <= 0) {
                    throw new IllegalArgumentException("maxRecords 与 pollTimeoutMs 必须为正整数");
                }
                return new KafkaFetchParams(topic, max, poll, reset);
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException("Kafka fetchSpec JSON 解析失败: " + e.getMessage());
            }
        }

        // 纯字符串视为 topic
        return new KafkaFetchParams(raw, defaultMax, defaultPoll, defaultReset);
    }

    private Properties buildClientProperties(JsonNode cfg, String clientId) {
        String bootstrap = cfg.path("bootstrapServers").asText("").trim();
        if (bootstrap.isEmpty()) {
            throw new IllegalArgumentException("bootstrapServers 不能为空");
        }

        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(AdminClientConfig.CLIENT_ID_CONFIG, clientId);
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, String.valueOf(KafkaConstants.REQUEST_TIMEOUT_MS));

        String security = cfg.path("securityProtocol").asText(KafkaConstants.SECURITY_PLAINTEXT).trim();
        if (!KafkaConstants.ALLOWED_SECURITY_PROTOCOLS.contains(security)) {
            security = KafkaConstants.SECURITY_PLAINTEXT;
        }
        props.put(org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, security);

        String user = cfg.path("saslUsername").asText("").trim();
        String mech = cfg.path("saslMechanism").asText("").trim();
        String pass = cfg.path("saslPassword").asText("");
        if (!user.isEmpty() && !mech.isEmpty()) {
            props.put(SaslConfigs.SASL_MECHANISM, mech);
            props.put(SaslConfigs.SASL_JAAS_CONFIG, buildJaas(mech, user, pass));
        }
        return props;
    }

    private static String buildJaas(String mechanism, String username, String password) {
        String escapedUser = escapeJaas(username);
        String escapedPass = escapeJaas(password);
        return switch (mechanism) {
            case KafkaConstants.SASL_PLAIN -> String.format(
                    "%s required username=\"%s\" password=\"%s\";",
                    KafkaConstants.JAAS_PLAIN_MODULE, escapedUser, escapedPass);
            case "SCRAM-SHA-256", "SCRAM-SHA-512" -> String.format(
                    "%s required username=\"%s\" password=\"%s\";",
                    KafkaConstants.JAAS_SCRAM_MODULE, escapedUser, escapedPass);
            default -> throw new IllegalArgumentException("暂不支持的 saslMechanism: " + mechanism +
                    "（支持 PLAIN、SCRAM-SHA-256、SCRAM-SHA-512）");
        };
    }

    private static String escapeJaas(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record KafkaFetchParams(String topic, int maxRecords, int pollTimeoutMs, String autoOffsetReset) {
    }
}
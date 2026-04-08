package com.example.datascreen.constant;

import java.util.Set;

/**
 * Kafka 客户端与安全相关常量。
 */
public final class KafkaConstants {

    private KafkaConstants() {
    }

    public static final Set<String> ALLOWED_SECURITY_PROTOCOLS =
            Set.of("PLAINTEXT", "SSL", "SASL_PLAINTEXT", "SASL_SSL");

    public static final String SECURITY_PLAINTEXT = "PLAINTEXT";

    public static final String SASL_PLAINTEXT = "SASL_PLAINTEXT";
    public static final String SASL_SSL = "SASL_SSL";

    public static final Set<String> ALLOWED_AUTO_OFFSET_RESET = Set.of("latest", "earliest");
    public static final String AUTO_OFFSET_LATEST = "latest";
    public static final String AUTO_OFFSET_EARLIEST = "earliest";

    public static final Set<String> SUPPORTED_SASL_MECHANISMS =
            Set.of("PLAIN", "SCRAM-SHA-256", "SCRAM-SHA-512");

    public static final String SASL_PLAIN = "PLAIN";

    public static final String JAAS_PLAIN_MODULE =
            "org.apache.kafka.common.security.plain.PlainLoginModule";
    public static final String JAAS_SCRAM_MODULE =
            "org.apache.kafka.common.security.scram.ScramLoginModule";

    public static final String CLIENT_ID_PREFIX_TEST = "data-screen-test-";
    public static final String CLIENT_ID_PREFIX_FETCH = "data-screen-fetch-";

    public static final int REQUEST_TIMEOUT_MS = 15_000;
    public static final int ADMIN_OPERATION_TIMEOUT_SECONDS = 15;

    public static final int CONSUMER_INITIAL_POLL_MS = 200;
    public static final int CONSUMER_POLL_SLICE_MS = 500;

    public static final int DEFAULT_MAX_RECORDS = 100;
    public static final int DEFAULT_POLL_TIMEOUT_MS = 8_000;
    public static final int MAX_POLL_RECORDS_CAP = 500;
}

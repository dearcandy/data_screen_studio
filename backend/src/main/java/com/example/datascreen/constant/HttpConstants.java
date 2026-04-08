package com.example.datascreen.constant;

import java.time.Duration;
import java.util.Set;

/**
 * HTTP 数据源相关常量（方法名、超时、WebClient 缓冲等）。
 */
public final class HttpConstants {

    private HttpConstants() {
    }

    public static final String METHOD_GET = "GET";
    public static final String METHOD_POST = "POST";

    public static final Set<String> ALLOWED_METHODS = Set.of(METHOD_GET, METHOD_POST);

    public static final String DEFAULT_PATH = "/";

    /** WebClient 默认最大缓冲（与取数场景一致，避免大响应失败） */
    public static final int WEB_CLIENT_MAX_IN_MEMORY_BYTES = 32 * 1024 * 1024;

    public static final Duration TEST_TIMEOUT = Duration.ofSeconds(15);
    public static final Duration FETCH_TIMEOUT = Duration.ofSeconds(30);
}

package com.example.datascreen.constant;

/**
 * JDBC 连接与 Statement 行为相关常量。
 */
public final class JdbcClientConstants {

    private JdbcClientConstants() {
    }

    /** {@link java.sql.Connection#isValid(int)} 秒数 */
    public static final int CONNECTION_VALID_TIMEOUT_SECONDS = 3;

    /** {@link java.sql.Statement#setQueryTimeout(int)} 秒数 */
    public static final int STATEMENT_QUERY_TIMEOUT_SECONDS = 30;
}

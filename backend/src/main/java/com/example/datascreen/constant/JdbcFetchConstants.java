package com.example.datascreen.constant;

import java.util.Set;

/**
 * 数据集 JDBC 取数：仅允许只读查询的前缀校验。
 */
public final class JdbcFetchConstants {

    private JdbcFetchConstants() {
    }

    public static final String MSG_SQL_REQUIRED = "请填写 SQL（数据集 fetchSpec）";
    public static final String MSG_SQL_READ_ONLY = "仅允许 SELECT 或 WITH 开头的查询语句";

    private static final Set<String> READONLY_STATEMENT_PREFIXES = Set.of("SELECT", "WITH");

    /**
     * 校验 fetchSpec 非空且以 SELECT / WITH 开头（忽略前导空白，大小写不敏感）。
     */
    public static String requireReadOnlySql(String fetchSpec) {
        String sql = fetchSpec != null ? fetchSpec.trim() : "";
        if (sql.isBlank()) {
            throw new IllegalArgumentException(MSG_SQL_REQUIRED);
        }
        String normalized = sql.stripLeading();
        boolean allowed = READONLY_STATEMENT_PREFIXES.stream()
                .anyMatch(keyword -> normalized.regionMatches(true, 0, keyword, 0, keyword.length()));
        if (!allowed) {
            throw new IllegalArgumentException(MSG_SQL_READ_ONLY);
        }
        return sql;
    }
}

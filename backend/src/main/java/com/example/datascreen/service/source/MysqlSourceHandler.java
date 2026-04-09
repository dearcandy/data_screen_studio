package com.example.datascreen.service.source;

import com.example.datascreen.constant.JdbcClientConstants;
import com.example.datascreen.constant.JdbcFetchConstants;
import com.example.datascreen.model.SourceType;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MySQL 数据源处理器
 */
@Component
public class MysqlSourceHandler implements DataSourceTypeHandler {

    @Override
    public SourceType supportType() {
        return SourceType.MYSQL;
    }

    @Override
    public void validateConfig(JsonNode cfg) {
        HandlerValidationSupport.checkUnknown(cfg, Set.of("host", "port", "database", "username", "password"));
        HandlerValidationSupport.require(cfg, "host", "port", "database", "username", "password");
        HandlerValidationSupport.requirePositiveInt(cfg); // 校验 port 为正整数
    }

    @Override
    public void testConnection(JsonNode cfg) throws SQLException {
        String url = JdbcUrlBuilder.buildMysqlUrl(cfg);
        String username = cfg.path("username").asText("");
        String password = cfg.path("password").asText("");
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            if (!conn.isValid(JdbcClientConstants.CONNECTION_VALID_TIMEOUT_SECONDS)) {
                throw new SQLException("数据库连接无效，请检查配置");
            }
        }
    }

    @Override
    public Object fetch(JsonNode cfg, String fetchSpec) throws SQLException {
        String sql = JdbcFetchConstants.requireReadOnlySql(fetchSpec);

        String url = JdbcUrlBuilder.buildMysqlUrl(cfg);
        String username = cfg.path("username").asText("");
        String password = cfg.path("password").asText("");

        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement()) {
            // 设置查询超时，防止慢查询阻塞
            stmt.setQueryTimeout(JdbcClientConstants.STATEMENT_QUERY_TIMEOUT_SECONDS);
            try (ResultSet rs = stmt.executeQuery(sql)) {
                return resultSetToList(rs);
            }
        }
    }

    /**
     * 将 ResultSet 转换为 List<Map<String, Object>>
     * 列名使用 getColumnLabel（支持别名），值为对应的 Java 对象
     */
    private List<Map<String, Object>> resultSetToList(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        List<Map<String, Object>> rows = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                Object value = rs.getObject(i);
                row.put(columnName, value);
            }
            rows.add(row);
        }
        return rows;
    }
}
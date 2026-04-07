package com.example.datascreen.service.source;

import com.example.datascreen.model.SourceType;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        HandlerValidationSupport.requirePositiveInt(cfg);
    }

    @Override
    public void testConnection(JsonNode cfg) throws Exception {
        try (Connection c = DriverManager.getConnection(JdbcUrlBuilder.buildMysqlUrl(cfg),
                cfg.path("username").asText(""),
                cfg.path("password").asText(""))) {
            c.isValid(3);
        }
    }

    @Override
    public Object fetch(JsonNode cfg, String fetchSpec) throws Exception {
        String sql = fetchSpec != null ? fetchSpec : "";
        if (sql.isBlank()) {
            throw new IllegalArgumentException("请填写 SQL（数据集 fetchSpec）");
        }
        String normalized = sql.stripLeading();
        if (!normalized.regionMatches(true, 0, "SELECT", 0, 6)
                && !normalized.regionMatches(true, 0, "WITH", 0, 4)) {
            throw new IllegalArgumentException("仅允许 SELECT / WITH 查询");
        }
        try (Connection conn = DriverManager.getConnection(JdbcUrlBuilder.buildMysqlUrl(cfg),
                cfg.path("username").asText(""),
                cfg.path("password").asText(""));
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return resultSetToList(rs);
        }
    }

    private List<Map<String, Object>> resultSetToList(ResultSet rs) throws Exception {
        ResultSetMetaData md = rs.getMetaData();
        int cols = md.getColumnCount();
        List<Map<String, Object>> rows = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= cols; i++) {
                row.put(md.getColumnLabel(i), rs.getObject(i));
            }
            rows.add(row);
        }
        return rows;
    }
}

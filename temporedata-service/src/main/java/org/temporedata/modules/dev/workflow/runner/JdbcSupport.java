package org.temporedata.modules.dev.workflow.runner;

import org.temporedata.common.util.Crypto;
import org.temporedata.modules.integration.datasource.entity.DatasourceEntity;
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

/**
 * Small JDBC helper reused by quality engine and node executors. Builds the JDBC
 * URL from a datasource and runs count / sample queries.
 */
@Component
public class JdbcSupport {

    private final Crypto crypto;

    public JdbcSupport(Crypto crypto) {
        this.crypto = crypto;
    }

    /** Run a query returning a single numeric value (e.g. COUNT). */
    public long count(DatasourceEntity ds, String sql) {
        try (Connection conn = open(ds);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (Exception e) {
            throw new RuntimeException("Query failed: " + e.getMessage(), e);
        }
    }

    /** Run a query selecting one column and return up to <code>limit</code> string samples. */
    public List<String> sample(DatasourceEntity ds, String sql, int limit) {
        List<String> out = new ArrayList<>();
        try (Connection conn = open(ds);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next() && out.size() < limit) {
                out.add(rs.getString(1));
            }
        } catch (Exception e) {
            throw new RuntimeException("Sample query failed: " + e.getMessage(), e);
        }
        return out;
    }

    /** Run a SELECT and return the full result set as {@code List<Map<column,value>>}. */
    public List<Map<String, Object>> query(DatasourceEntity ds, String sql) {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = open(ds);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= cols; i++) {
                    row.put(md.getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
            }
        } catch (Exception e) {
            throw new RuntimeException("Query failed: " + e.getMessage(), e);
        }
        return rows;
    }

    private Connection open(DatasourceEntity ds) throws Exception {
        String password = crypto.decrypt(ds.getPassword());
        return DriverManager.getConnection(buildJdbcUrl(ds), ds.getUsername(), password);
    }

    public String buildJdbcUrl(DatasourceEntity entity) {
        String type = entity.getType();
        String host = entity.getHost();
        Integer port = entity.getPort();
        String database = entity.getDatabase();
        String params = entity.getParams();

        String url;
        if ("MYSQL".equalsIgnoreCase(type)) {
            url = String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false",
                    host, port, database);
        } else if ("POSTGRESQL".equalsIgnoreCase(type)) {
            url = String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
        } else if ("ORACLE".equalsIgnoreCase(type)) {
            url = String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, database);
        } else {
            throw new IllegalStateException("Unsupported datasource type: " + type);
        }
        if (params != null && !params.isBlank()) {
            url += (url.contains("?") ? "&" : "?") + params;
        }
        return url;
    }
}
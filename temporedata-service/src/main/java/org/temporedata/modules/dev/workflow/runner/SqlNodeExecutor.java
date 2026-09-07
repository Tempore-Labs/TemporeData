package org.temporedata.modules.dev.workflow.runner;

import lombok.extern.slf4j.Slf4j;
import org.temporedata.common.util.Crypto;
import org.temporedata.modules.dev.workflow.entity.WorkflowNodeEntity;
import org.temporedata.modules.integration.datasource.entity.DatasourceEntity;
import org.temporedata.modules.integration.datasource.repository.DatasourceRepository;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Set;

/**
 * SQL node executor. Executes against a configured datasource over JDBC when a
 * datasource id is set; otherwise falls back to a deterministic simulated
 * execution so the DAG runtime remains demonstrable without a real database.
 */
@Slf4j
@Component
public class SqlNodeExecutor implements NodeExecutor {

    private final DatasourceRepository datasourceRepository;
    private final Crypto crypto;

    public SqlNodeExecutor(DatasourceRepository datasourceRepository, Crypto crypto) {
        this.datasourceRepository = datasourceRepository;
        this.crypto = crypto;
    }

    @Override
    public Set<String> type() {
        return Set.of("SQL", "HIVE", "SPARKSQL");
    }

    @Override
    public NodeRunResult execute(WorkflowNodeEntity node, NodeExecutionContext ctx) throws Exception {
        String sql = node.getSql();
        if (sql == null || sql.isBlank()) {
            return NodeRunResult.failed("SQL node has empty sql");
        }
        // Inject business date placeholders (P1-4).
        if (ctx.getBizDate() != null) {
            sql = sql.replace("${biz_date}", ctx.getBizDate())
                    .replace("${biz_date_ym}", ctx.getBizDate().replace("-", "").substring(0, 6));
        }

        if (node.getDatasourceId() == null || node.getDatasourceId().isBlank()) {
            // No datasource configured - deterministic simulated execution.
            long rows = (sql.hashCode() & 0x7fffffff) % 1000 + 1;
            return NodeRunResult.ok("simulated execution (no datasource), rows=" + rows, rows);
        }

        DatasourceEntity ds = datasourceRepository.findById(node.getDatasourceId())
                .orElseThrow(() -> new IllegalStateException("Datasource not found: " + node.getDatasourceId()));

        String url = buildJdbcUrl(ds);
        String password = crypto.decrypt(ds.getPassword());
        boolean select = isSelect(sql);

        try (Connection conn = DriverManager.getConnection(url, ds.getUsername(), password)) {
            long rows;
            try (Statement st = conn.createStatement()) {
                if (select) {
                    rows = countRows(st, sql);
                } else {
                    rows = st.executeUpdate(sql);
                }
            }
            return NodeRunResult.ok("executed against datasource " + ds.getName()
                    + (select ? ", selected rows=" + rows : ", affected rows=" + rows), rows);
        }
    }

    private long countRows(Statement st, String sql) throws Exception {
        long n = 0;
        try (ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                if (++n > 1_000_000) break;
            }
        }
        return n;
    }

    private boolean isSelect(String sql) {
        String s = sql.trim();
        return s.regionMatches(true, 0, "select", 0, 6)
                || s.regionMatches(true, 0, "with", 0, 4);
    }

    private String buildJdbcUrl(DatasourceEntity entity) {
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
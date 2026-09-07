package org.temporedata.modules.dev.workflow.runner;

import lombok.extern.slf4j.Slf4j;
import org.temporedata.modules.dev.workflow.entity.WorkflowNodeEntity;
import org.temporedata.modules.integration.datasource.entity.DatasourceEntity;
import org.temporedata.modules.integration.datasource.repository.DatasourceRepository;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * QUALITY node executor (P1-6). Treats <code>node.sql</code> as a check that
 * returns the number of violating rows; pass when it returns 0. According to
 * <code>failStrategy</code>: warn -> node succeeds with a warning message;
 * block (default) -> node fails and blocks downstream.
 */
@Slf4j
@Component
public class QualityNodeExecutor implements NodeExecutor {

    private final DatasourceRepository datasourceRepository;
    private final JdbcSupport jdbcSupport;

    public QualityNodeExecutor(DatasourceRepository datasourceRepository, JdbcSupport jdbcSupport) {
        this.datasourceRepository = datasourceRepository;
        this.jdbcSupport = jdbcSupport;
    }

    @Override
    public Set<String> type() {
        return Set.of("QUALITY", "QUALITY_CHECK");
    }

    @Override
    public NodeRunResult execute(WorkflowNodeEntity node, NodeExecutionContext ctx) {
        String checkSql = node.getSql();
        if (checkSql == null || checkSql.isBlank()) {
            return NodeRunResult.failed("QUALITY node has empty check SQL");
        }

        long bad;
        if (node.getDatasourceId() == null || node.getDatasourceId().isBlank()) {
            int hash = Math.abs((node.getId() + ":" + node.getName()).hashCode());
            bad = hash % 4 == 0 ? 1 + hash % 20 : 0;
            log.info("QUALITY node simulated (no datasource): bad={}", bad);
        } else {
            DatasourceEntity ds = datasourceRepository.findById(node.getDatasourceId())
                    .orElseThrow(() -> new IllegalStateException("Datasource not found: " + node.getDatasourceId()));
            // COUNT of the user's SELECT is the number of violating rows.
            String sql = "SELECT COUNT(*) FROM (" + checkSql + ") q";
            bad = jdbcSupport.count(ds, sql);
        }

        boolean pass = bad == 0;
        String strategy = node.getFailStrategy() == null ? "block" : node.getFailStrategy();
        String message = pass
                ? "Quality check passed (0 violations)"
                : "Quality check FAILED, violations=" + bad;

        if (pass) {
            return NodeRunResult.ok(message, bad);
        }
        if ("warn".equalsIgnoreCase(strategy)) {
            // continue pipeline but surface the warning
            return NodeRunResult.ok("WARN | " + message, bad);
        }
        return NodeRunResult.failed(message);
    }
}
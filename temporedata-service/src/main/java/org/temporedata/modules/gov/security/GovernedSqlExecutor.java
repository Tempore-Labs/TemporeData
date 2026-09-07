package org.temporedata.modules.gov.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.api.datasource.DatasourceType;
import org.temporedata.api.datasource.sql.Dialect;
import org.temporedata.api.dev.workflow.SqlParseRes;
import org.temporedata.api.gov.security.AccessPolicy;
import org.temporedata.api.gov.security.GovExecResult;
import org.temporedata.api.gov.security.GovernedResult;
import org.temporedata.modules.dev.workflow.runner.JdbcSupport;
import org.temporedata.modules.dev.workflow.runner.SqlFirewall;
import org.temporedata.modules.dev.workflow.runner.SqlLineageParser;
import org.temporedata.modules.integration.datasource.entity.DatasourceEntity;
import org.temporedata.modules.integration.datasource.repository.DatasourceRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * GovernedSqlExecutor — the single execution engine shared by every data-exit point
 * (P2, 数据访问控制与动态脱敏执行链 v1.0 §3): QueryService, DataApi invoke, governed export.
 *
 * <p>Pipeline: validate SELECT → derive table → {@link AccessPolicyResolver} decision →
 * {@link SqlPlanRewriter} Stage-B pushdown (mask down to the data source when the dialect
 * supports it) → {@link JdbcSupport} execute → {@link DataAccessGovernor} Stage-A app-side
 * fallback (prune + stream-mask). Fail-closed when the policy or SQL cannot be decided.</p>
 *
 * <p>P3 hardening: performance-budget circuit breaker (result row cap + app-side mask
 * coverage warning) and asynchronous governance audit (§4.6).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GovernedSqlExecutor {

    /** Cap rows returned to the caller to avoid OOM / oversized payloads (P3 breaker). */
    private static final int MAX_RESULT_ROWS = 500;

    /** Budget: if app-side masked columns exceed this, log a coverage warning (P3 §4.7). */
    private static final int APP_MASK_BUDGET = 5;

    private final DatasourceRepository datasourceRepository;
    private final JdbcSupport jdbcSupport;
    private final SqlFirewall sqlFirewall;
    private final GovernanceAudit governanceAudit;
    private final AccessPolicyResolver accessPolicyResolver;
    private final DataAccessGovernor governor;
    private final SqlPlanRewriter sqlPlanRewriter;
    private final SqlLineageParser sqlLineageParser;

    /**
     * Execute a governed SELECT for the current authenticated caller.
     *
     * @throws BusinessException for non-SELECT SQL or unknown datasource.
     */
    public GovExecResult execute(String sql, String datasourceId) {
        return execute(sql, datasourceId, "QUERY");
    }

    public GovExecResult execute(String sql, String datasourceId, String action) {
        long startMs = System.currentTimeMillis();
        GovExecResult out = new GovExecResult();

        try {
            validateSql(sql);
            DatasourceEntity ds = datasourceRepository.findById(datasourceId)
                    .orElseThrow(() -> new BusinessException("Datasource not found: " + datasourceId));

            String table = deriveTable(sql);
            AccessPolicy policy = (table == null)
                    ? new AccessPolicy()
                    : accessPolicyResolver.resolve(datasourceId, table);
            int maskCols = policy.getDesensitizeRules() == null ? 0 : policy.getDesensitizeRules().size();
            out.setMaskCols(maskCols);
            out.setTable(table);

            if (policy.isDenied()) {
                out.setDenied(true);
                out.setDurationMs(System.currentTimeMillis() - startMs);
                audit(action, out, "DENY");
                return out;
            }

            // Stage B: push mask expressions down to the data source when possible
            boolean pushed = false;
            String execSql = sql;
            if (isMysqlFamily(ds) && maskCols > 0) {
                Optional<String> rewritten = sqlPlanRewriter.rewrite(sql, policy);
                if (rewritten.isPresent()) {
                    execSql = rewritten.get();
                    pushed = true;
                }
            }
            out.setExecutedSql(execSql);
            out.setPushed(pushed);

            // SQL firewall (P3): the effective SQL — original or pushdown-rewritten — must
            // pass a dialect-aware injection check before it is executed. Fail-closed.
            List<String> violations = sqlFirewall.validate(execSql, dialectOf(ds));
            if (!violations.isEmpty()) {
                log.warn("Governed query BLOCKED by firewall: ds={}, violations={}", datasourceId, violations);
                out.setDurationMs(System.currentTimeMillis() - startMs);
                audit(action, out, "DENY");
                throw new BusinessException("SQL 被防火墙拦截: " + String.join("; ", violations));
            }

            List<Map<String, Object>> rows = jdbcSupport.query(ds, execSql);
            out.setFetchedRowCount(rows.size());

            // P3 circuit breaker: enforce result-size budget
            int returnedRows = rows.size();
            if (returnedRows > MAX_RESULT_ROWS) {
                rows = new ArrayList<>(rows.subList(0, MAX_RESULT_ROWS));
                out.setTruncated(true);
                returnedRows = MAX_RESULT_ROWS;
            }
            List<String> columns = rows.isEmpty() ? new ArrayList<>() : new ArrayList<>(rows.get(0).keySet());

            List<String> outCols;
            List<Map<String, Object>> outRows;
            if (pushed) {
                outCols = columns;
                outRows = rows;
            } else {
                GovernedResult gr = governor.govern(policy, columns, rows);
                outCols = gr.getColumns();
                outRows = gr.getRows();
                // P3 §4.7 mask-coverage budget: warn when app-side masking is heavy
                if (maskCols > APP_MASK_BUDGET) {
                    log.warn("Governed query app-side mask above budget: ds={}, table={}, maskCols={}",
                            datasourceId, table, maskCols);
                }
            }

            out.setColumns(outCols);
            out.setRows(outRows);
            out.setRowCount(outRows.size());
            out.setDurationMs(System.currentTimeMillis() - startMs);

            audit(action, out, outRows.isEmpty() && maskCols > 0 ? "FAILED" : "ALLOW");

            log.debug("Governed execute: ds={}, table={}, fetched={}, returned={}, maskCols={}, pushdown={}, truncated={}, {}ms",
                    datasourceId, table, out.getFetchedRowCount(), out.getRowCount(), maskCols, pushed,
                    out.isTruncated(), out.getDurationMs());
            return out;
        } catch (Exception e) {
            log.error("Governed execute FAILED: ds={}, action={}, err={}", datasourceId, action, e.getMessage());
            out.setDurationMs(System.currentTimeMillis() - startMs);
            audit(action, out, "FAILED");
            if (e instanceof BusinessException) {
                throw (BusinessException) e;
            }
            throw new BusinessException("查询执行失败: " + e.getMessage());
        }
    }

    private void audit(String action, GovExecResult out, String status) {
        try {
            String resource = (out.getTable() == null ? "unknown" : out.getTable());
            String operator = currentUser();
            String ip = clientIp();
            governanceAudit.auditAsync(action, resource, operator, ip, status,
                    out.getMaskCols(), out.isPushed(), out.isTruncated());
        } catch (Exception ignored) {
            // governance audit itself must never break the query
        }
    }

    private void validateSql(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new BusinessException("SQL cannot be empty");
        }
        String up = sql.trim().toUpperCase();
        if (Pattern.matches("^\\s*(DROP|ALTER|TRUNCATE|CREATE|INSERT|UPDATE|DELETE|GRANT|REVOKE).*", up)) {
            throw new BusinessException("Only SELECT queries are allowed for execution");
        }
    }

    private String deriveTable(String sql) {
        try {
            SqlParseRes res = sqlLineageParser.parse(sql);
            if (res != null && res.getSources() != null && !res.getSources().isEmpty()) {
                return res.getSources().get(0);
            }
        } catch (Exception ignored) {
            // no governance table -> no policy
        }
        return null;
    }

    /** MySQL-protocol datasources share the CONCAT/SUBSTRING/MD5 expression dialect. */
    private boolean isMysqlFamily(DatasourceEntity ds) {
        if (ds == null || ds.getType() == null) return false;
        switch (ds.getType().trim().toUpperCase()) {
            case "MYSQL":
            case "DORIS":
            case "STARROCKS":
            case "OCEANBASE":
            case "HIVE":
                return true;
            default:
                return false;
        }
    }

    private Dialect dialectOf(DatasourceEntity ds) {
        DatasourceType type = ds == null ? null : DatasourceType.of(ds.getType());
        return Dialect.from(type);
    }

    private String currentUser() {
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            return auth != null && auth.getName() != null ? auth.getName() : "anonymous";
        } catch (Exception e) {
            return "anonymous";
        }
    }

    private String clientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                String ip = req.getHeader("X-Forwarded-For");
                if (ip != null && !ip.isBlank()) return ip.split(",")[0].trim();
                return req.getRemoteAddr();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return null;
    }
}
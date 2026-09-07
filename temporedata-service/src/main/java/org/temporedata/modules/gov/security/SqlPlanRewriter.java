package org.temporedata.modules.gov.security;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLAllColumnExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.parser.SQLParserUtils;
import com.alibaba.druid.sql.SQLUtils;
import org.temporedata.api.gov.security.AccessPolicy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SqlPlanRewriter (P1 / Stage B, 数据访问控制与动态脱敏执行链 v1.0 §4.2).
 * <p>
 * Pushes security processing down to the data source by rewriting an explicit-column
 * {@code SELECT} into one that computes the mask expressions in the DB, e.g.
 * {@code SELECT account, ...} becomes
 * {@code SELECT CONCAT(SUBSTRING(account,1,4),'****',SUBSTRING(account,-4)) AS account, ...}.
 * Only MySQL-family expressions are emitted; the caller gates on datasource type.
 * <p>
 * Fail-closed: if any masked column cannot be rewritten as a plain selected column
 * (e.g. {@code SELECT *}, function expressions, unknown rule), this returns
 * {@link Optional#empty()} so the caller falls back to application-side masking
 * ({@link DataAccessGovernor} Stage A) rather than silently leaking plaintext.
 */
@Component
public class SqlPlanRewriter {

    /** Rewrite a MySQL-family SELECT to push mask expressions down; empty = fall back. */
    public Optional<String> rewrite(String sql, AccessPolicy policy) {
        Map<String, String> rules = policy == null ? null : policy.getDesensitizeRules();
        if (rules == null || rules.isEmpty()) {
            return Optional.empty();
        }
        Map<String, String> patterns = policy == null ? null : policy.getMaskPatterns();
        try {
            SQLStatement stmt = SQLParserUtils.createSQLStatementParser(sql, DbType.mysql).parseStatement();
            SQLSelectQueryBlock block = queryBlock(stmt);
            if (block == null) {
                return Optional.empty();
            }
            List<SQLSelectItem> items = block.getSelectList();
            if (items == null) {
                return Optional.empty();
            }

            boolean sawStar = false;
            for (SQLSelectItem it : items) {
                if (it.getExpr() instanceof SQLAllColumnExpr) {
                    sawStar = true;
                    continue;
                }
                String outName = outputName(it);
                if (outName == null) {
                    continue; // non-column expression: not a pushable column
                }
                String rule = rules.get(outName);
                if (rule == null) {
                    continue;
                }
                if (!(it.getExpr() instanceof SQLIdentifierExpr)) {
                    return Optional.empty(); // masked column used via expression: fall back
                }
                String expr = mysqlMaskExpr(outName, rule, patterns == null ? null : patterns.get(outName));
                if (expr == null) {
                    return Optional.empty(); // unsupported rule: fall back to app-side masking
                }
                it.setExpr(SQLUtils.toSQLExpr(expr, DbType.mysql));
                it.setAlias(outName);
            }

            // Fail-closed: a bare `SELECT *` lists masked columns at runtime but cannot be
            // rewritten here; push nothing and fall back to app-side masking (DataAccessGovernor).
            if (sawStar) {
                return Optional.empty();
            }

            // Explicit masked columns all rewritten (or no masked column in the list): safe.
            return Optional.of(stmt.toString());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private SQLSelectQueryBlock queryBlock(SQLStatement stmt) {
        if (!(stmt instanceof SQLSelectStatement)) {
            return null;
        }
        SQLSelectQueryBlock block = ((SQLSelectStatement) stmt).getSelect().getFirstQueryBlock();
        return block;
    }

    private String outputName(SQLSelectItem it) {
        if (it.getAlias() != null) {
            return it.getAlias();
        }
        if (it.getExpr() instanceof SQLIdentifierExpr) {
            return ((SQLIdentifierExpr) it.getExpr()).getName();
        }
        return null;
    }

    /** MySQL-native mask expression for a column; null = not pushable for this rule. */
    private String mysqlMaskExpr(String col, String rule, String pattern) {
        String c = "`" + col.replace("`", "") + "`";
        switch (rule.trim().toUpperCase()) {
            case "NAME":
                return "CONCAT(SUBSTRING(" + c + ",1,1),'**')";
            case "PHONE":
                return "CONCAT(SUBSTRING(" + c + ",1,3),'****',SUBSTRING(" + c + ",-4))";
            case "ID_CARD":
                return "CONCAT(SUBSTRING(" + c + ",1,6),'********',SUBSTRING(" + c + ",-4))";
            case "BANK_CARD":
                return "CONCAT(SUBSTRING(" + c + ",1,4),' **** **** ',SUBSTRING(" + c + ",-4))";
            case "EMAIL":
                return "CONCAT(SUBSTRING(" + c + ",1,1),'***',SUBSTRING(" + c + ",LOCATE('@'," + c + ")))";
            case "CUSTOM": {
                String p = (pattern == null || pattern.isBlank()) ? "****" : pattern;
                return "CONCAT(SUBSTRING(" + c + ",1,4),'" + p + "',SUBSTRING(" + c + ",-4))";
            }
            case "HASH":
                return "MD5(" + c + ")";
            default:
                return null;
        }
    }
}
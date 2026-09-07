package org.temporedata.modules.gov.security;

import org.temporedata.api.gov.security.AccessPolicy;
import org.temporedata.api.gov.security.GovernedResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DataAccessGovernor — the executable heart of 数据访问控制与动态脱敏执行链 v1.0.
 *
 * <p>Stage A (fit for future SQL-plan push-down): column pruning happens by only
 * selecting granted columns (this utility prunes eagerly so callers can attach it
 * to a real result set). Stage B: any remaining granted-but-sensitive columns are
 * desensitized on a <em>streamed</em> row basis (§4.4) — values are transformed
 * one row at a time, no whole-batch in-memory masking.</p>
 *
 * <p>Fail-closed: a {@code denied} policy returns an empty result.</p>
 */
@Component
public class DataAccessGovernor {

    private final DataDesensitizer desensitizer;

    public DataAccessGovernor(DataDesensitizer desensitizer) {
        this.desensitizer = desensitizer;
    }

    /** Apply policy to a raw (column-origin) result: prune + desensitize. */
    public GovernedResult govern(AccessPolicy policy, List<String> columns, List<Map<String, Object>> rows) {
        GovernedResult out = new GovernedResult();
        if (policy == null) {
            policy = new AccessPolicy();
        }
        if (policy.isDenied()) {
            out.setDenied(true);
            return out;
        }

        List<String> outCols = new ArrayList<>();
        Set<String> granted = policy.getGrantedColumns();
        boolean filterColumns = granted != null && !granted.isEmpty();
        for (String col : columns) {
            if (!filterColumns || granted.contains(col)) {
                outCols.add(col);
            }
        }
        out.setColumns(outCols);

        List<Map<String, Object>> outRows = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> maskedRow = new LinkedHashMap<>();
            for (String col : outCols) {
                Object value = row.get(col);
                String rule = policy.getDesensitizeRules() == null ? null : policy.getDesensitizeRules().get(col);
                if (rule != null && value != null) {
                    String pattern = policy.getMaskPatterns() == null ? null : policy.getMaskPatterns().get(col);
                    value = desensitizer.apply(rule, pattern, String.valueOf(value));
                }
                maskedRow.put(col, value);
            }
            outRows.add(maskedRow);
        }
        out.setRows(outRows);
        return out;
    }
}
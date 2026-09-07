package org.temporedata.api.gov.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Result of a governed SQL execution through the data-access control & dynamic
 * desensitization chain (轻舟数据云 数据访问控制与动态脱敏执行链 v1.0).
 *
 * <p>Shared by every exit point (QueryService / DataApi invoke / governed export)
 * so the governance decision, execution meta and the final (pruned + masked) payload
 * are consistent wherever a request leaves the platform.</p>
 */
public class GovExecResult {

    /** Final columns after pruning. */
    private List<String> columns = new ArrayList<>();

    /** Final rows after pruning + masking. */
    private List<Map<String, Object>> rows = new ArrayList<>();

    /** Raw row count fetched (before any cap). */
    private int fetchedRowCount;

    /** Row count returned to the caller (after cap). */
    private int rowCount;

    /** Wall-clock duration of the governed execution in ms. */
    private long durationMs;

    /** true when masking was pushed down to the data source (Stage B). */
    private boolean pushed;

    /** Number of columns subject to an active mask rule. */
    private int maskCols;

    /** true when access was denied (fail-closed) -> empty result. */
    private boolean denied;

    /** The table derived from the SQL (null when not determinable). */
    private String table;

    /** The effective SQL that was executed against the data source. */
    private String executedSql;

    /** true when the cell-count result exceeded the configured row cap (circuit breaker). */
    private boolean truncated;

    public List<String> getColumns() { return columns; }
    public void setColumns(List<String> v) { this.columns = v == null ? new ArrayList<>() : v; }

    public List<Map<String, Object>> getRows() { return rows; }
    public void setRows(List<Map<String, Object>> v) { this.rows = v == null ? new ArrayList<>() : v; }

    public int getFetchedRowCount() { return fetchedRowCount; }
    public void setFetchedRowCount(int v) { this.fetchedRowCount = v; }

    public int getRowCount() { return rowCount; }
    public void setRowCount(int v) { this.rowCount = v; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long v) { this.durationMs = v; }

    public boolean isPushed() { return pushed; }
    public void setPushed(boolean v) { this.pushed = v; }

    public int getMaskCols() { return maskCols; }
    public void setMaskCols(int v) { this.maskCols = v; }

    public boolean isDenied() { return denied; }
    public void setDenied(boolean v) { this.denied = v; }

    public String getTable() { return table; }
    public void setTable(String v) { this.table = v; }

    public String getExecutedSql() { return executedSql; }
    public void setExecutedSql(String v) { this.executedSql = v; }

    public boolean isTruncated() { return truncated; }
    public void setTruncated(boolean v) { this.truncated = v; }
}
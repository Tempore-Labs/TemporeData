package org.temporedata.api.datasource.sql;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Dialect-agnostic result of SQL parsing into table-level lineage.
 * Produced by {@link SqlParser} implementations and kept stable across
 * parsers (JSqlParser / optional Druid) so callers can stay consistent.
 */
@Data
public class SqlParseResult {

    private boolean success;
    private String message;
    /** Normalised statement kind, e.g. INSERT / CREATE_TABLE / SELECT. */
    private String sqlType;
    /** Table produced by the statement, or null for plain SELECT. */
    private String targetTable;
    /** Tables read by the statement. */
    private List<String> sources = new ArrayList<>();
    /** All referenced tables. */
    private List<String> tables = new ArrayList<>();
    /** Column references seen in the statement (best-effort, P3). */
    private List<String> columns = new ArrayList<>();
    /** Column-level lineage hops (best-effort, P3). */
    private List<ColumnLineage> columnLineage = new ArrayList<>();
    /** Dialect used for this parse (routing hint). */
    private Dialect dialect;
}
package org.temporedata.api.dev.workflow;

import lombok.Data;
import org.temporedata.api.datasource.sql.ColumnLineage;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of parsing a SQL statement into table-level lineage (P1-5).
 */
@Data
public class SqlParseRes {

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
    private boolean success;
    private String message;
}
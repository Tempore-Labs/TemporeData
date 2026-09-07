package org.temporedata.api.datasource.sql;

import lombok.Data;

/**
 * A single column-level lineage hop: a target column produced from a source
 * column. Best-effort, derived from INSERT...SELECT position mapping (P3).
 */
@Data
public class ColumnLineage {

    private String sourceTable;
    private String sourceColumn;
    private String targetTable;
    private String targetColumn;
}
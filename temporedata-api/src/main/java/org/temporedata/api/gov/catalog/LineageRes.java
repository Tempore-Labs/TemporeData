package org.temporedata.api.gov.catalog;

import lombok.Data;

import java.util.List;

/**
 * Table lineage: upstream and downstream relationships.
 */
@Data
public class LineageRes {

    private String tableId;
    private String tableName;
    private List<LineageNode> upstream;   // tables that feed into this table
    private List<LineageNode> downstream; // tables that consume this table

    @Data
    public static class LineageNode {
        private String tableId;
        private String tableName;
        private String relationType; // SYNC, INGESTION, WORKFLOW
        private String relationName; // e.g. sync task name
    }
}
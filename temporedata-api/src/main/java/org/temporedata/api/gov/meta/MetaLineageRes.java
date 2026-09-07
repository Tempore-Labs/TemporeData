package org.temporedata.api.gov.meta;

import lombok.Data;
import java.util.List;

/**
 * Enhanced table lineage with multi-level traversal.
 */
@Data
public class MetaLineageRes {

    private String tableId;
    private String tableName;
    private List<LineageNode> upstream;
    private List<LineageNode> downstream;
    private List<LineageEdge> edges;

    @Data
    public static class LineageNode {
        private String tableId;
        private String tableName;
        private String datasourceName;
        private String relationType; // SYNC, INGESTION, WORKFLOW, QUERY, METADATA
        private String relationName;
        private Integer depth; // distance from root table
    }

    @Data
    public static class LineageEdge {
        private String id;
        private String sourceTableId;
        private String sourceTableName;
        private String targetTableId;
        private String targetTableName;
        private String relationType;
        private String relationName;
    }
}
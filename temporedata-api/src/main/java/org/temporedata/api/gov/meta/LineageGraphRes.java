package org.temporedata.api.gov.meta;

import lombok.Data;
import java.util.List;

/**
 * Full lineage graph data for ECharts force-directed graph visualization.
 * Contains nodes (tables) and links (lineage relationships).
 */
@Data
public class LineageGraphRes {

    private String rootTableId;
    private String rootTableName;
    private List<GraphNode> nodes;
    private List<GraphLink> links;

    /** Statistics for the lineage graph */
    private int totalUpstream;
    private int totalDownstream;
    private int maxDepth;

    @Data
    public static class GraphNode {
        private String id;
        private String name;
        private String nodeType;   // TABLE / COLUMN / TASK ...
        private String datasourceName;
        private String category; // "root", "upstream", "downstream", "external"
        private Integer depth;   // distance from root
        private Integer layer;   // layer for layered layout (negative upstream, positive downstream)
        private String symbolSize; // visualization size hint
        private Long rowCount;
        private Long columnCount;
    }

    @Data
    public static class GraphLink {
        private String source;
        private String target;
        private String relationType; // SYNC, INGESTION, WORKFLOW, QUERY, MANUAL
        private String relationName;
        private String transformationSql;
        private String description;
        private List<FieldLink> fieldLinks; // field-level details
    }

    @Data
    public static class FieldLink {
        private String sourceColumn;
        private String targetColumn;
        private String transformationLogic;
    }
}
package org.temporedata.api.gov.meta;

import lombok.Data;
import java.util.List;

/**
 * Impact analysis result: what tables are affected downstream if this table changes.
 */
@Data
public class ImpactAnalysisRes {

    private String tableId;
    private String tableName;
    private int totalImpacted;
    private int maxDepth;
    private List<ImpactPath> paths;

    @Data
    public static class ImpactPath {
        private String tableId;
        private String tableName;
        private String datasourceName;
        private int depth;
        private String relationType;
        private String relationName;
        private List<String> pathChain; // table names along the path
    }
}
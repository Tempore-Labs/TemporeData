package org.temporedata.api.gov.meta;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LineageAnalysisRes {

    private String tableName;
    private int totalNodes;
    private int totalEdges;
    private boolean hasCycle;
    private List<String> cycleNodes = new ArrayList<>();
    private double avgDepth;
    private int maxDepth;
    private int maxFanOut;
    private String summary;
}
package org.temporedata.api.gov.lineage;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LineagePathRes {

    private String sourceId;
    private String targetId;
    private String sourceName;
    private String targetName;
    private boolean found;
    private List<String> path = new ArrayList<>();
    private List<PathHop> hops = new ArrayList<>();

    @Data
    public static class PathHop {
        private String source;
        private String target;
        private String edgeType;
        private String taskName;
    }
}
package org.temporedata.api.dev.workflow;

import lombok.Data;

import java.util.List;

/**
 * Synchronous run response for a workflow DAG execution (P0-2).
 */
@Data
public class WorkflowRunRes {

    private String instanceId;
    private String status;
    private String startTime;
    private String endTime;
    private List<NodeRunItem> nodeResults;

    @Data
    public static class NodeRunItem {
        private String nodeId;
        private String nodeName;
        private String status;
        private String errorMsg;
        private long durationMs;
    }
}
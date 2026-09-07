package org.temporedata.api.dev.workflow;

import lombok.Data;

import java.util.List;

/**
 * Workflow execution result.
 */
@Data
public class WorkflowExecuteRes {

    private String executeId;

    private String status;

    private List<NodeExecuteItem> nodeResults;

    private String startTime;

    private String endTime;

    @Data
    public static class NodeExecuteItem {
        private String nodeId;
        private String nodeName;
        private String status;
        private String errorMsg;
        private long durationMs;
    }
}
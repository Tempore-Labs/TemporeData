package org.temporedata.modules.dev.workflow.runner;

import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.Map;

/**
 * Runtime context passed to a node executor.
 */
@Data
@Builder
public class NodeExecutionContext {

    /** The workflow run instance id. */
    private final String instanceId;

    /** The node instance id being executed. */
    private final String nodeInstanceId;

    /** Business date injected by the scheduler (P1). */
    private final String bizDate;

    /** Results of upstream nodes (nodeId -> result), only successful ones are present. */
    @Builder.Default
    private final Map<String, NodeRunResult> upstream = Collections.emptyMap();
}
package org.temporedata.modules.dev.workflow.runner;

import lombok.Builder;
import lombok.Data;

/**
 * Outcome of running a single workflow node.
 */
@Data
@Builder
public class NodeRunResult {

    /** Whether the node completed successfully. */
    private final boolean success;

    /** Human-readable result message / summary. */
    private final String message;

    /** Affected / produced rows (used by conditions via node.&lt;id&gt;.rows). */
    private final long affectedRows;

    public static NodeRunResult ok(String message, long rows) {
        return NodeRunResult.builder().success(true).message(message).affectedRows(rows).build();
    }

    public static NodeRunResult failed(String message) {
        return NodeRunResult.builder().success(false).message(message).affectedRows(0).build();
    }
}
package org.temporedata.modules.dev.workflow.runner;

import org.temporedata.modules.dev.workflow.entity.WorkflowNodeEntity;

import java.util.Collections;
import java.util.Set;

/**
 * Strategy that executes a single node of a DAG. New engines (Spark, Sync,
 * Quality...) implement this and register via {@link #type()}.
 */
public interface NodeExecutor {

    /**
     * Execute the node against the given context.
     */
    NodeRunResult execute(WorkflowNodeEntity node, NodeExecutionContext ctx) throws Exception;

    /**
     * Node type key this executor handles (e.g. "SQL", "HTTP"). Empty set means
     * "no specific type" (used by the fallback placeholder).
     */
    default Set<String> type() {
        return Collections.emptySet();
    }
}
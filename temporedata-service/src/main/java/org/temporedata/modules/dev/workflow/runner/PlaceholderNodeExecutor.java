package org.temporedata.modules.dev.workflow.runner;

import org.temporedata.modules.dev.workflow.entity.WorkflowNodeEntity;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Fallback executor for node types without an engine yet (SPARK, SHELL, QUALITY,
 * SYNC...). Reports FAILED so the DAG runtime still advances deterministically.
 */
@Component
public class PlaceholderNodeExecutor implements NodeExecutor {

    @Override
    public NodeRunResult execute(WorkflowNodeEntity node, NodeExecutionContext ctx) {
        String type = node.getType() == null ? "UNKNOWN" : node.getType();
        return NodeRunResult.failed("No executor implemented for node type: " + type);
    }

    @Override
    public Set<String> type() {
        return Set.of();
    }
}
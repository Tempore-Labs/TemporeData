package org.temporedata.api.executor;

public interface JobExecutor {
    String type();

    ExecutionResult submit(ExecutionContext context, JobDefinition definition);

    void cancel(ExecutionContext context);

    ExecutionStatus status(ExecutionContext context);
}

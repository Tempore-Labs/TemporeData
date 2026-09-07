# 14 Job State Machine

```text
DRAFT
  │ enable
  ▼
ENABLED
  │ run
  ▼
QUEUED
  │ dispatch
  ▼
RUNNING
 ┌┴───────────┐
 ▼            ▼
SUCCESS      FAILED
               │ retry
               ▼
             QUEUED

RUNNING → CANCELLED
```

禁止：

```text
SUCCESS → RUNNING
CANCELLED → RUNNING
DELETED → ENABLED
```

实现：

```java
public final class JobExecutionStateMachine {

    public void transition(JobExecution execution, JobExecutionStatus target) {
        JobExecutionStatus current = execution.status();

        if (!allowed(current, target)) {
            throw new DomainException(
                "JOB_INVALID_STATE_TRANSITION",
                current + " -> " + target
            );
        }

        execution.changeStatus(target);
    }
}
```

所有状态变化必须：

- 写数据库
- 写审计
- 发布 Domain Event
- 更新 Metric

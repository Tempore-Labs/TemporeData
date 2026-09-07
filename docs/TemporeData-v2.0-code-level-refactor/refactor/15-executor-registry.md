# 15 Executor Registry

## Registry

```java
public interface JobExecutorRegistry {
    JobExecutor get(String type);
    boolean supports(String type);
}
```

实现：

```java
@Component
public class DefaultJobExecutorRegistry implements JobExecutorRegistry {

    private final Map<String, JobExecutor> executors;

    public DefaultJobExecutorRegistry(List<JobExecutor> list) {
        this.executors = list.stream()
            .collect(Collectors.toUnmodifiableMap(
                JobExecutor::type,
                Function.identity()
            ));
    }

    @Override
    public JobExecutor get(String type) {
        JobExecutor executor = executors.get(type);
        if (executor == null) {
            throw new DomainException(
                "JOB_EXECUTOR_UNAVAILABLE",
                "No executor for: " + type
            );
        }
        return executor;
    }

    @Override
    public boolean supports(String type) {
        return executors.containsKey(type);
    }
}
```

## Executor lifecycle

```text
resolve
→ validate
→ authorize
→ submit
→ persist execution
→ observe
→ finalize
→ audit
```

Executor 不负责：

- 用户权限判断
- Agent 推理
- 业务状态决策
- 修改租户策略

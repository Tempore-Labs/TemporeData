# 07 Job 与 Executor 执行引擎

## 1. Job 统一模型

```text
JobDefinition
 ├── Schedule
 ├── Execution
 │    ├── Step
 │    ├── Resource
 │    ├── Log
 │    ├── Event
 │    └── Result
 └── Policy
```

统一替代 SparkJob、FlinkJob、RealJob、TaskDefine 等碎片化入口。

## 2. Executor SPI

```java
public interface JobExecutor {
    String type();

    ExecutionResult submit(ExecutionContext context, JobDefinition definition);

    void cancel(ExecutionContext context);

    ExecutionStatus status(ExecutionContext context);
}
```

实现：

```text
ShellExecutor
SshExecutor
JdbcExecutor
SparkExecutor
FlinkExecutor
KubernetesExecutor
DockerExecutor
HttpExecutor
PluginExecutor
```

## 3. Collector SPI

Collector 与 Executor 分离：

```java
public interface Collector {
    String type();
    CollectionSnapshot collect(CollectorContext context);
}
```

Collector：

```text
HostCollector
HadoopCollector
SparkCollector
FlinkCollector
KafkaCollector
HiveCollector
TrinoCollector
KubernetesCollector
```

## 4. 生命周期

```text
CREATE
 → VALIDATE
 → QUEUE
 → START
 → RUNNING
 → SUCCESS / FAILED / CANCELLED
```

每次状态变化产生事件。

## 5. Retry

只对明确可重试错误重试：

```text
NETWORK_TIMEOUT
TEMPORARY_UNAVAILABLE
LOCK_CONFLICT
```

不可自动重试：

```text
PERMISSION_DENIED
INVALID_SQL
DATA_VALIDATION_FAILED
USER_CANCELLED
```

指数退避：

```text
1s → 2s → 4s → 8s → max 60s
```

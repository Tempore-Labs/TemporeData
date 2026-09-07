# 08 Observability / Incident / RCA

## 1. 四类遥测

```text
Metric
Log
Event
Trace
```

统一关联：

```text
tenantId
resourceId
jobId
executionId
traceId
timestamp
```

## 2. 告警模型

```text
AlertRule
   ↓
AlertEvent
   ↓
AlertGroup
   ↓
Incident
   ↓
Notification
```

Alert 不直接等同 Incident。

## 3. Incident 状态

```text
OPEN
ACKNOWLEDGED
INVESTIGATING
MITIGATED
RESOLVED
CLOSED
```

## 4. Fingerprint

由以下稳定字段计算：

```text
tenant + resource + rule + dimension
```

避免同一故障生成大量 Incident。

## 5. RCA Engine

输入：

```text
metrics
logs
events
topology
dependencies
recent changes
job executions
```

输出：

```text
rootCause
confidence
evidence[]
impact[]
recommendations[]
safeActions[]
```

RCA 必须保存证据，不允许只给“模型猜测”。

## 6. Change Correlation

事故发生前 N 分钟的：

- 发布
- 配置变更
- 扩容/缩容
- Job 部署
- Runbook 执行

进入 RCA 时间线。

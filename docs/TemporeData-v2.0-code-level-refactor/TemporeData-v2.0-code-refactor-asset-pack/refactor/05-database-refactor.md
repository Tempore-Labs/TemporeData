# 05 数据库重构

## 1. 新命名

新表统一：

```text
temporedata_*
```

旧 `zy_*` 暂不删除，通过 Adapter/兼容查询逐步迁移。

## 2. 核心表

```text
temporedata_cluster
temporedata_cluster_node
temporedata_cluster_service
temporedata_compute_engine
temporedata_job
temporedata_job_schedule
temporedata_job_execution
temporedata_job_execution_step
temporedata_metric
temporedata_event
temporedata_log_index
temporedata_alert_rule
temporedata_alert_event
temporedata_incident
temporedata_runbook
temporedata_runbook_execution
temporedata_action_approval
temporedata_action_audit
```

## 3. 通用字段

```text
id varchar(36)
tenant_id varchar(36)
created_at datetime
updated_at datetime
created_by varchar(36)
updated_by varchar(36)
version bigint
deleted boolean
```

## 4. 关键索引

Job Execution：

```text
(tenant_id, job_id, created_at)
(tenant_id, status, created_at)
(execution_id)
```

Incident：

```text
(tenant_id, status, severity, updated_at)
(tenant_id, fingerprint)
```

Metric：

```text
(tenant_id, resource_id, metric_name, ts)
```

## 5. Flyway

```text
V30__temporedata_cluster.sql
V31__temporedata_job.sql
V32__temporedata_observability.sql
V33__temporedata_incident.sql
V34__temporedata_automation.sql
V35__temporedata_action_audit.sql
V36__legacy_mapping.sql
```

迁移必须可重复执行测试；生产禁止手工修改 schema。

## 6. 数据保留

建议默认：

```text
Metric: 30d
Log index: 7d
Job execution: 180d
Audit: 365d+
Incident: 365d
```

实际策略由租户配置覆盖。

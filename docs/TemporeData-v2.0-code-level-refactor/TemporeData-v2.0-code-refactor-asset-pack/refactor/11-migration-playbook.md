# 11 分阶段迁移手册

## Phase 0 — 基线

- 锁定现有源码树
- 增加 ArchUnit
- 建立 API contract
- 建立数据库快照
- 建立关键 E2E

## Phase 1 — 基础域

新增：

```text
system
cluster
monitoring
```

先实现新的 Dashboard、Cluster、Node、Service、Metric。

## Phase 2 — Job

把：

```text
workflow
schedule
SparkJob
FlinkJob
RealJob
TaskDefine
```

统一到 Job。

旧 API 通过 Adapter 调用新 JobApplicationService。

## Phase 3 — Incident

替换：

```text
AlarmConfig
AlarmRecord
Baseline
```

形成：

```text
AlertRule → AlertEvent → Incident
```

## Phase 4 — Automation

引入：

```text
Runbook
ActionPlan
Approval
Executor
Audit
```

先人工批准，禁止自动修复。

## Phase 5 — Data

重新归位：

```text
Datasource
Metadata
Catalog
Lineage
Quality
Security
```

成熟的 lineage 实现直接复用，不重写核心算法。

## Phase 6 — Agent

先只读：

```text
Observe
Explain
Recommend
```

再逐步开放：

```text
Execute with approval
```

最后才考虑低风险自动化。

## Phase 7 — 删除旧域

满足以下条件才删除 legacy：

- v1 API 覆盖率 100%
- 前端不再调用 legacy
- 数据迁移完成
- 审计可追溯
- E2E 通过
- 两个版本周期无回滚

## 回滚原则

每个 Flyway 迁移必须有兼容窗口；应用发布优先于 destructive schema change。

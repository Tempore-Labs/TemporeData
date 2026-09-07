# 22 Recommended Code Refactor Sequence

```text
01 Freeze baseline
02 Add architecture tests
03 Add API v1 contracts
04 Add new package skeleton
05 Add legacy adapters
06 Migrate system
07 Migrate cluster
08 Migrate monitoring
09 Introduce Job model
10 Introduce Executor SPI
11 Migrate workflow/schedule
12 Migrate logs
13 Introduce Alert/Incident
14 Introduce Runbook/Approval
15 Move datasource/metadata
16 Preserve/rehome lineage
17 Add Ops Agent read-only
18 Add controlled execution
19 Switch UI navigation
20 Remove legacy
```

## Important

不要先重写 59 个 Vue 页面。

先建立新的 Layout + Feature API + 核心 15~20 个页面，再逐步迁移旧页面。

不要先重写所有 Java Service。

先迁移领域边界和接口，再迁移实现。

这能显著降低 v2.0 重构风险。

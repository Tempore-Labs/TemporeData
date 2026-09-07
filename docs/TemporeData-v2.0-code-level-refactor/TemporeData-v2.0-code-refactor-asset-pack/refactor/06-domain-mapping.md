# 06 旧域 → 新域映射

| 旧域 | 新域 | 处理 |
|---|---|---|
| sys.auth | system.auth | 保留 |
| sys.account | system.account | 合并 |
| sys.menu | system.permission | 重组 |
| sys.agent | automation.agent | 改名 |
| ops.cluster | cluster | 核心提升 |
| ops.engine | compute | 核心提升 |
| ops.real | compute/job | 拆分 |
| ops.sync | job/data | 拆分 |
| ops.ingestion | data | 归位 |
| ops.alarm | incident | 升级 |
| ops.dashboard | dashboard | 产品顶层 |
| ops.audit | system.audit | 横切 |
| dev.workflow | job | 统一 Job |
| dev.schedule | job.schedule | 归位 |
| dev.query | data/query | 归位 |
| dev.approval | system/automation | 拆分 |
| gov.meta | data.metadata | 归位 |
| gov.lineage | data.lineage | 保留成熟实现 |
| gov.catalog | data.catalog | 归位 |
| gov.quality | data.quality | 归位 |
| gov.security | data.security | 保留治理能力 |
| svc.service | data.service | 可选 |
| svc.report | data.report | 低优先级 |
| asset.* | data.asset | 降级为辅助域 |
| integration.datasource | data.datasource | 与 Connector 统一 |

## 核心变化

原先“研发/资产/服务/治理”是产品中心；v2.0 改成：

```text
Cluster
Compute
Job
Monitoring
Incident
Automation
Data
```

数据治理不删除，而是从“产品主导航”下沉到 Data。

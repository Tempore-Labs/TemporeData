# TemporeData v2.0 代码级重构资产包

本资产包以现有 **Java + Vue 完整源码树设计** 为基线，将产品升级为：

> **TemporeData — 轻量级企业大数据运维与 DataOps 平台**

## 本次重构原则

1. **不推倒重来**：保留现有 Maven 多模块、Spring Boot、Vue3、Flyway、数据源 SPI、SQL Parser、血缘、RBAC、审计、缓存/锁、Docker/CI。
2. **业务域重组**：从 `sys/dev/ops/gov/svc/asset/integration` 重组为 `system/cluster/compute/job/monitoring/logging/incident/automation/data`。
3. **控制平面优先**：产品主线变为 Observe → Understand → Operate → Automate。
4. **执行安全闭环**：Agent 只能提出 Action Plan，不能绕过 Policy/Approval 直接执行高风险操作。
5. **渐进式迁移**：API 与数据库采用兼容层 + Flyway 双轨迁移，允许旧功能持续运行。

## 资产包目录

- `design/`：原有架构、数据源、SQL Parser、Metadata、Lineage、Security 等设计
- `product/`：产品设计
- `ui/`：UI/UX 与血缘设计
- `refactor/01-refactor-master-plan.md`：总体重构方案
- `refactor/02-backend-code-architecture.md`：Java 后端代码级架构
- `refactor/03-frontend-code-architecture.md`：Vue 前端代码级架构
- `refactor/04-api-contract.md`：API 契约与版本化
- `refactor/05-database-refactor.md`：数据库与 Flyway 重构
- `refactor/06-domain-mapping.md`：旧域 → 新域映射
- `refactor/07-execution-engine.md`：Job/Executor 执行引擎
- `refactor/08-observability-incident.md`：监控、告警、Incident、RCA
- `refactor/09-agent-safety.md`：Ops Agent 安全执行链
- `refactor/10-testing-quality.md`：测试与架构门禁
- `refactor/11-migration-playbook.md`：分阶段迁移手册
- `refactor/target-source-tree.txt`：目标源码树
- `refactor/package-rules.md`：Java 包与依赖规则
- `refactor/adr/`：关键架构决策记录
- `refactor/sql/`：迁移 SQL 模板
- `refactor/examples/`：代码骨架与接口示例

## 推荐实施顺序

P0 → 领域包迁移与契约统一 → Cluster/Monitoring/Job 基础域 → Incident/Automation → Data/Lineage 归位 → Agent → 删除兼容代码。

> 本包是“代码级重构设计”，不是对原项目源代码的直接改写；它定义了可以直接交给开发团队执行的目标结构、接口、迁移和质量门禁。

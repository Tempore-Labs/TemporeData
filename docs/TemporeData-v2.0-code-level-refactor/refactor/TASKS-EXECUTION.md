# TemporeData v2.0 代码级重构 · 细化执行任务追踪

> 粒度原则：每个叶子任务小而可独立执行——标出**精确改动/新建的路径**、依赖、验收。勾选完成。
> 顺序 = 下↑上依赖；请勿跳级。Refactor Sequence 见 `22-code-refactor-sequence.md`；迁移阶段见 `11-migration-playbook.md`。

图例：`[D]`=依赖前置项 · `[P]`=并行可做 · `✅=验收`。

---

## Phase 0 · 基线与护栏

### 0.1 冻结基线
- [ ] **P0.1.1** 打基线分支/tag（`git tag v2.0-refactor-baseline`），记录当前 `mvn --version`、`pom.xml` 模块清单。`✅ tag 存在，可随时回退`
- [ ] **P0.1.2** DB 基线快照：导出 `temporedata_server` 当前 Flyway `schema_version` + 核心表 DDL 到 `docs/refactor/baseline-db.sql`。`✅ 快照文件含 schema_version 与表清单`
- [ ] **P0.1.3** E2E 基线：跑通现有 `scripts/lineage-walkthrough.cjs` + 现有 `npm run build`，记录通过基线。`✅ 两项绿，作为回归参照`

### 0.2 架构守护（ArchUnit）
- [ ] **P0.2.1** `temporedata-service/pom.xml` 加 `com.tngtech.archunit:archunit-junit5`（test scope）。`✅ 依赖可解析，`mvn -pl temporedata-service -am test-compile` 通过`
- [ ] **P0.2.2** 新增 `temporedata-service/src/test/java/org/temporedata/arch/ArchUnitRules.java`：规则①Controller 不直连 Repository；②Domain 包不依赖 `org.springframework.web`；③`*.controller` 不得 `repository.save/jdbcTemplate`；④包依赖单向。`✅ 规则编译通过`
- [ ] **P0.2.3** 新建 `temporedata-service/src/test/java/org/temporedata/arch/LayeringArchTest.java`，在现有代码上先运行（预期有违规列表，**记录为 legacy 豁免清单**）。`✅ 输出豁免清单文件`
- [ ] **P0.2.4** 建豁免机制：ArchUnit `ImportOption.DoNotInclude` 或 `withPackageClasses` 只对**新域包**启用严格规则（val0 不炸现有代码）。`✅ 新包规则生效、旧豁免白名单化`
- [ ] **P0.2.5** CI：`ci.yml` 加 `mvn -pl temporedata-service -am test` 已覆盖 arch 测试，确认 arch test 被跑（或加专用 job）。`✅ CI job 含 arch 测试`

### 0.3 API v1 契约骨架
- [ ] **P0.3.1** 公共响应 `org.temporedata.api.contract.ApiResponse<T>{code,msg,data,traceId,requestId}` 兼容现有 `BaseResponse`。`✅ 新增类 + 单测`
- [ ] **P0.3.2** 统一错误码枚举 `org.temporedata.api.contract.ErrorCode`（`JOB_NOT_FOUND`…），异常→响应映射器。`✅ 枚举+映射单测`
- [ ] **P0.3.3** `Idempotency-Key` 过滤器：读 header→缓存判定→响应头回写；`@Idempotent` 注解。`✅ 过滤器+单元测试`
- [ ] **P0.3.4** `/api/v1/health` 探针（permitAll）+ `/api/v1/ping`（auth）。`✅ curl 返回 ApiResponse 结构`
- [ ] **P0.3.5** OpenAPI：`springdoc` 暴露 `/v3/api-docs` + `/swagger-ui`；写契约基线快照。`✅ 文档可导出`
- [ ] **P0.3.6** 前端 `src/services/http/client.ts`：统一 `http` 封装（baseURL=/api/v1、traceId、错误码映射）。`✅ 替换性封装 + 冒烟调用`

---

## Phase 1 · 基础领域（system / cluster / monitoring）

### 1.1 后端新包骨架
- [ ] **P1.1.1** `temporedata-service` 下建 9 域目录骨架：`system/cluster/compute/job/monitoring/logging/incident/automation/data`，各含空包 `controller/application/domain/repository/dto/mapper/executor/event`（用 `.gitkeep`）。`✅ 目录存在、层级一致`
- [ ] **P1.1.2** 建 `org.temporedata.service.system` 最小 `SystemPingController`（v1）走 `SystemApplicationService`。`✅ 端到端调用链通过（不经 Repository）`
- [ ] **P1.1.3** 建 `cluster` 域骨架：`ClusterController -> ClusterApplicationService -> ClusterRepository(接口) -> JpaClusterRepository(实现)` + `ClusterDomainEntity`。`✅ ArchUnit 新域规则零违反`

### 1.2 system / cluster 域实现
- [ ] **P1.2.1** system：`dto/SystemRes`、`application/SystemApplicationService`（事务+审计）、`AuditService` 记录 `SYS_*`。`✅ 单测`
- [ ] **P1.2.2** cluster：`CreateClusterReq/ClusterRes`、`Cluster` 领域不变量（状态机 `pending→active→down`）、`ClusterRepository` 接口 + JPA 实现。`✅ 单测+集成测试`
- [ ] **P1.2.3** monitoring：`MetricRes`、`MonitoringService`（拉取指标 + TSDB 可选适配）。`✅ 单测`
- [ ] **P1.2.4** 事件：`ClusterNodeDownEvent`（Spring `ApplicationEvent` 发布）。`✅ 事件监听单测`

### 1.3 Flyway：cluster / monitoring 表
- [ ] **P1.3.1** 新建迁移 `temporedata-server/src/main/resources/db/migration/V33__v2_cluster.sql`：`t_cluster`（含 `tenant_id`、`version`（乐观锁INT）、`deleted`(软删)、索引）。`✅ Flyway 同步 + 可回滚窗口`
- [ ] **P1.3.2** 新建 `V34__v2_metric.sql`：`t_metric` 表 + 索引。`✅ 迁移绿`
- [ ] **P1.3.3** Entity 用 `@Version`（乐观锁）+ `@SQLDelete`/filter（软删）。`✅ 并发更新单测`

### 1.4 前端 Layout + Feature 骨架（壳）— ✅ 壳已建，核心页清单见 [FRONTEND-CORE-PAGES.md](FRONTEND-CORE-PAGES.md)
- [x] **P1.4.1** `temporedata-ui/src/layouts/AppLayoutV2.vue` + `navigation.js`。`✅ 新建未接线，不影响现有应用`
- [x] **P1.4.2** `features/{dashboard,jobs}` 示例（api/types/routes/views，走 `services/http` 调 `/api/v1`）。`✅ 示例`
- [x] **P1.4.3** 通用组件 `components/base/{DataTable,MetricCard,StatusBadge}.vue` + `CommandPalette.vue`。`✅`（ResourceDrawer 待补）
- [x] **P1.4.4** `app/routes.js`（v2 路由表）+ `features/index.js`。`✅ 已建未挂载`
- [x] **P1.4.5** 权限门禁 `permissions/index.js`（hasPerm + canExecute + 高风险清单）。`✅`
- [x] 配套：`services/http.js` · `composables/usePageState.js` · `design-system/tokens.css` · `app/README.md`。`✅`
- [ ] 接线（切导航时）：`router.addRoute(v2RootRoute)`，使 `/v2/**` 可用；其余 7 个 feature、ResourceDrawer 按清单补齐。`✅ 待接线`

---

## Phase 2 · Job 统一

- [ ] **P2.1** 建 `job` 域：`JobAggregate`（type=WORKFLOW/SCHEDULE/SPARK/FLINK/REALTIME/TASKDEFINE）、`JobStatus` 状态机由 Domain 提供 `enable/disable/cancel`（不变量校验）。`✅ 状态机单测`
- [ ] **P2.2** `job/executor/JobExecutor` 接口 + `registry`（types→executor 映射）；`Idempotent-Key` 应用于执行。`✅ registry 单测`
- [ ] **P2.3** `JobApplicationService`：`create/update/delete/execute/submit` 编排；`@Transactional`。`✅ 单测+集成测试`
- [ ] **P2.4** legacy 迁移：`workflow/schedule/SparkJob/FlinkJob/RealJob/TaskDefine` 现有实现迁入 `job`，旧 Controller 仅留 Adapter 调 `JobApplicationService`。`✅ 旧 API 行为不变（E2E 通过）`
- [ ] **P2.5** Flyway `V35__v2_job.sql`（t_job + t_job_execution，含 tenant/version/deleted/索引 + 状态列）。`✅ 迁移绿`
- [ ] **P2.6** 前端 `features/jobs/`：`api/types/store/views{JobList,JobDetail,JobExecution}` + `JobStatus.vue`（健康状态映射）。`✅ Playwright 冒烟`

---

## Phase 3 · Incident

- [ ] **P3.1** `incident` 域：`AlertRule`（配置）→`AlertEvent`（触发记录）→`Incident`（聚合）。迁移 `AlarmConfig/AlarmRecord/Baseline`。`✅ 单测`
- [ ] **P3.2** 事件发布 `AlertTriggeredEvent/IncidentCreatedEvent`（跨域走事件，不直调）。`✅ 事件链单测`
- [ ] **P3.3** Flyway `V36__v2_incident.sql`。`✅ 迁移绿`
- [ ] **P3.4** 前端 `features/incidents/`（列表+详情 Drawer）。`✅ 冒烟`

---

## Phase 4 · Automation（受控执行）

- [ ] **P4.1** `automation` 域：`Runbook/Approval/Executor/Audit`；先人工批准（默认阻止自动执行）。`✅ 权限+审批单测`
- [ ] **P4.2** `ActionPlan`：`ActionPlan{actions[],approvalRequired}` + 执行器解析。`✅ 解析单测`
- [ ] **P4.3** 安全链实现：Agent→`Policy`→`Permission`→`Approval`→`Executor`→`Audit`（ADR-002）；高风险动作强制 `Action Preview`。`✅ 链式单测 + 越权拒绝测试`
- [ ] **P4.4** 前端 `features/automation/`：runbooks/executions（approval 工作流）。`✅ 冒烟`

---

## Phase 5 · Data

- [ ] **P5.1** `data` 域骨架，迁移 Datasource/Metadata/Catalog/Lineage/Quality/Security 归位。**成熟 lineage 复用现有实现，不重写算法。**`✅ 现有 lineage E2E 不回归`
- [ ] **P5.2** 迁移 `module-datasource`/`module-lineage` 现有服务的包位置到 `data`（ACL Adapter 包一层）。`✅ 旧 API 不变`
- [ ] **P5.3** 前端 `features/data/`（datasources/metadata/lineage 页面挂 Feature 结构）。`✅ 冒烟`

---

## Phase 6 · Agent

- [ ] **P6.1** Agent 只读模式：`Observe/Explain/Recommend` 端点（不落执行）。`✅ 只读端点+权限`
- [ ] **P6.2** 受控执行：Agent 生成 `ActionPlan` → 走 P4.3 安全链；`Execute with approval` 开启。`✅ 端到端审批流`
- [ ] **P6.3** 低风险自动化（最后）：白名单动作自动执行。`✅ 白名单单测`

---

## Phase 7 · 前端迁移 + 弃旧

> 壳已建、核心 15–20 页迁移清单见 [FRONTEND-CORE-PAGES.md](FRONTEND-CORE-PAGES.md)（§4 含 17 页 + 每页迁移集 ⑦项）。

- [ ] **P7.1** 接线 `v2RootRoute` → `/v2/**` 可用；新 Layout/CommandPalette 设为默认导航；旧页面按 15–20 核心页清单逐步平移进 feature（不重写）。`✅ 详见 FRONTEND-CORE-PAGES`
- [ ] **P7.2** 每页迁移集：`①路由并入v2 ②api.js走/api/v1 ③types ④permission gate ⑤loading/empty/error ⑥高风险→Action Preview ⑦Playwright`。`✅ 逐页达标`
- [ ] **P7.3** 删除 legacy（仅当）：v1 API 覆盖率 100%、前端不再调 legacy、数据迁移完成、审计可追溯、E2E 过、两版本周期无回滚。`✅ 删除+全 E2E 绿`

---

## 横切 · 每域通用 checklist（新域增量）

- [ ] **X1** 后端：API v1 / DTO↔Entity 分离 / Repository 接口 / Application+Domain / 事件 / Audit / Permission / 幂等 / 单测+集成+权限+审计+契约测 / ArchUnit。`✅ 域 DoD 全绿`
- [ ] **X2** 前端：feature（api/types/routes/store）+ views + components + permission gate + loading/empty/error + Playwright。`✅`
- [ ] **X3** DB：Flyway + 兼容回滚窗口 + tenant_id + 乐观锁 + 软删 + 索引。`✅`
- [ ] **X4** 发布：OpenAPI diff / SBOM / 安全扫描 / E2E / Docker / 单 jar 验证 / 回滚测试。`✅`

---

## 执行顺序建议（最小可交付切分）
```
Phase0(0.1→0.3) → Phase1(1.1→1.3) → 前端1.4 / Phase2 → Phase3 → Phase4 → Phase5 → Phase6 → Phase7
每完成一个叶子 update 勾选；每阶段结束跑 arch + build + E2E 全绿再进入下一阶段。
```
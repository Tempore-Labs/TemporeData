# 元数据管理模块自动化设计（Metadata）

## 1. 现状与问题

旧 `gov/meta` 模块存在三个根本问题：**采集是模拟的**（`MetaService.sync()` 硬编码 `sampleTables/sampleColumns` 生成假表/列）、**数据模型是单一宽表**（`zy_meta` 超大冗余列）、**无自动化闭环**（无采集调度、无增量/变更感知、无联动）。

**目标**：建立自动、真实、可联动的元数据管理 —— 真实 JDBC 采集、表/列结构分层、自动化管线（采集→后处理）、业务联动（血缘/分级/脱敏/质量/目录）。

---

## 2. 数据模型重设计（结构分层）

### 2.1 `zy_meta_table`（表级）

```
id, datasource_id, schema_name, table_name, table_comment,
owner, create_time, last_sync_time, row_count, data_size, partition_key,
tags(JSON), data_level_code(数据分级), sensitive_flags(JSON), has_lineage,
upstream_count, downstream_count, source_type(BUILTIN/UPLOAD/MANUAL),
status(ACTIVE/CHANGED/ARCHIVED), version, checksum
```

### 2.2 `zy_meta_column`（列级）

```
id, table_id(FK), column_name, column_type, column_size, decimal_digits,
nullable, default_value, comment, primary_key, ordinal_position,
mask_rule_id(引用 zy_mask_rule), data_level_code, sensitive_flag,
derived(是否自动派生的默认规则), status, version
```

> 取代单表 `zy_meta` 宽表冗余；血缘/影响分析单独走血缘表，不再存进元数据宽表。

### 2.3 采集会话（可选 `zy_meta_sync_log`）

```
id, datasource_id, sync_type(FULL/INCREMENT), status, started_at, finished_at,
tables_discovered, columns_discovered, errors, sync_target(表结构hash)
```

用于可观测地与幂等恢复。

---

## 3. 自动化采集管线

```
调度(定时/手动) ──► MetaCollector ──► 会话 ──► 归一化 ──► Diff ──► 落库 ──► 后处理管道
   ▲                                                                      │
   └────────────── 触发下一次/重试 ◄──── 变更事件/缓存失效 ◄────────────────┘
```

| 阶段 | 职责 |
|---|---|
| `MetaCollector` | 用 `DatasourceProcessor.getConnection` 打开数据源 → `DatabaseMetaData.getTables/getColumns` 读**真实**表/列/主键/注释/大小 |
| 会话 `MetaSyncLog` | 记录采集目标与成败，支持增量重跑与恢复 |
| 归一化 `MetaNormalizer` | 适配方言差异（catalog/schema 语义），产出统一 `MetaTable/MetaColumn` |
| 变更检测 `MetaDiffer` | 与库中旧 `version+checksum` 比对 → 产出 `新增/删除/类型变更/注释变更` 集合 |
| 落库 + 失效缓存 | upsert 表/列 + `@CacheEvict(META_TABLES/META_COLUMNS)` |
| **后处理管道** `MetaPipeline` | 血缘关联、敏感/分级、脱敏规则派生、质量与目录联动 |

---

## 4. 调度与增量（自动化驱动）

- **调度**：复用 Quartz/`SchedulerCoordinator`，新增"元数据采集任务"：手动触发、按数据源定时轮询（cron）、变更感知增量。
- **增量**：凭 `checksum`（表结构 hash）判断是否变化；未变更跳过写库。
- **重试/幂等**：采集会话失败可重跑（增量 up），不产生重复脏数据。
- **并行**：多数据源并行采集（线程池/分片），大库只拉结构（`LIMIT`/cursor），不拉数据。

---

## 5. 业务自动化联动（贴合业务的核心）

| 联动 | 自动化动作 |
|---|---|
| **血缘** | 新表/列 → 复用 `SqlLineageParser`/工作流血缘同步，自动建上下游并刷新 `upstream/downstream_count` |
| **敏感/分级** | 按列名/类型字典（`name/phone/id_card/email/amount`）自动派生 `sensitive_flags/data_level_code`；支持人工覆盖 |
| **脱敏规则** | 高敏列自动在 `zy_mask_rule` 建默认规则（PHONE/NAME/ID_CARD/…），与 `AccessPolicyResolver` 打通 |
| **质量规则** | 结构变更 → 标记关联质量规则 `INVALID` 并告警 |
| **资产/目录** | 新表自动入 `zy_catalog`/资产目录、打标签、入 `MyData` 默认授权队列 |
| **影响分析** | 复用 `ImpactAnalysis`，结构变更 → 输出受影响下游链路 |

**后处理管道** `MetaPipeline`：一个 `List<MetaPostProcessor>` 按优先级执行（血缘→分级→脱敏→质量→目录），新增联动只加一个 Processor，可插拔。

---

## 6. 统一编排 `MetaCoordinator`

- `trigger/status/log`：手动触发采集、查看会话与进度、失败重试。
- 面向服务：`getTable/diff/simpleReport` 等查询（缓存）。
- 事件：变更事件发布（供质量/审计/资产订阅）。

---

## 7. 涉及文件 / 迁移

- **新增（service `gov/meta`）**：`MetaCollector`、`MetaNormalizer`、`MetaDiffer`、`MetaPipeline`（`MetaPostProcessor` SPI）、`MetaSyncLogEntity`；实体 `MetaTableEntity`、`MetaColumnEntity`；`MetaCoordinator`。
- **改造**：`MetaService.sync` 从 mock 改为走 `MetaCollector`；拆 `MetaEntity` → `MetaTable/MetaColumn`。
- **复用**：`DatasourceProcessor`、`SqlLineageParser`、`MaskRule`/`AccessPolicyResolver`、`DataLevel`/`DataCategory`、`QualityRuleEngine`、`SchedulerCoordinator`、缓存。

---

## 8. 分阶段落地（状态：✅ 全部落地）

| 阶段 | 交付 | 状态 |
|---|---|---|
| P0 | 实体拆分 + `MetaCollector` 真实 JDBC 采集（替换 mock）+ 增删查 + 缓存 | ✅ |
| P1 | 调度与增量（checksum diff + 会话 + 定时）+ `MetaDiffer` 变更集 | ✅ |
| P2 | 后处理管道：血缘关联、敏感/分级派生、脱敏规则默认生成 | ✅ |
| P3 | 质量/目录/资产联动 + 影响分析 + 变更事件订阅；移除 `zy_meta` 兼容表 | ✅ |

### 落地明细

- **P0**：迁移 `V23__meta_layered.sql` 建 `zy_meta_table/zy_meta_column`；实体 + 仓库；`MetaCollector.DatabaseMetaData` 真实读取（自动敏感标记）；端点 `/api/public/meta/{collect,tables,columns}`（验证：真采 172 表/2135 列；`zy_user` 敏感列自动标记）。
- **P1**：迁移 `V24__meta_sync_incremental.sql`：`zy_meta_table.checksum` + `zy_meta_sync_log`；`MetaCollector` checksum 增量（未变跳过写库）+ stale 不物理删 + 会话记录；`MetaSyncScheduler`（`@Scheduled`，cron `${temporedata.meta.sync.cron}`）。
- **P2**：`MetaPostProcessor` SPI + `MetaPipeline`（`@Order` 排序、单处理器失败隔离）；`SensitiveClassifierPostProcessor`（列名字典自动定级 P0-P3 + 敏感标记）；`DefaultMaskRulePostProcessor`（敏感列自动生成默认脱敏规则，接 `AccessPolicyResolver`）。验证：`zy_mask_rule.name` 自动定级 P2 → 自动建 `NAME` 规则 → 查询自动脱敏 `张三→张**`。
- **P3**：迁移 `V25__meta_change.sql` 变更事件表 `zy_meta_change`；`MetaCollector` 计算并持久化 `ADD/CHANGE/REMOVE`；`ChangeLinkPostProcessor`（表状态 `STRUCT_CHANGED/REMOVED`；质量规则失效原生 SQL）。验证：ALTER 加列 → `structChanged` → `CHANGE` 事件 → 表 `STRUCT_CHANGED` → 质量 `INVALID`。
- **P3 续**：`CatalogAssetPostProcessor`（Order 5）：采集后把表/列整合进 `zy_catalog` 作 `datasource→schema→table→column` 节点，按稳定 metadata id 复用（保留人工治理覆盖），陈旧目录行删除 → 幂等；表/列自动打分级标签；MyData 默认授权队列（配 `temporedata.meta.catalog.mydata.owner` 后新表自动入 `zy_mydata`）。**采集稳定性**：`MetaCollector` 改为协调式合并（未变化原样保留、只更新变化、删除已消失）。验证：plat-mysql 稳定 174 表/2154 列；目录幂等 2330 条。
- **P4**：`MetaCoordinator` 按注册数据源真实采集；新端点 `POST /api/public/meta/collect-ds/{id}`、`POST /api/public/meta/collect-all`、`GET /api/public/meta/overview`；前端 `Meta.vue` 重写读真实 `zy_meta_table/zy_meta_column`。备注：注册 MYSQL 数据源密码以 `temporedata.crypto.key`（AES-128-ECB）重加密可连通。
- **P5**：删除旧 `MetaEntity/MetaService/MetaController/MetaRepository` 与 `/api/meta/*` mock，`DROP TABLE zy_meta`；`LineageService.rebuild()` 改读真实 `zy_workflow_lineage` + `zy_meta_table`；`DashboardOverviewService` 改读真实 `zy_meta_table`（只采结构 → `totalRows=0` 符合设计）；清理孤儿表 `zy_meta_unified/zy_meta_lineage/zy_meta_field_lineage`；`zy_meta*` 仅剩 `zy_meta_table/zy_meta_column/zy_meta_sync_log/zy_meta_change`。

---

## 9. 风险与边界

- 方言/jdbc catalog 差异：`MetaNormalizer` 收敛，采集失败单数据源降级不阻断全局。
- 与旧 `zy_meta` 宽表兼容：迁移期双写/回退，稳定后已清理。
- 非目标：本版不做数据采样（仅结构）、不做列级数据血缘入库的深度加工（由工作流血缘承接）。
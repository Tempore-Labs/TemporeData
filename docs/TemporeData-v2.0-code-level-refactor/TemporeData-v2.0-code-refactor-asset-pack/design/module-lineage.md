# 数据血缘模块设计（Lineage · 后端）

> 归并原《temporedata-数据血缘模块重新设计_v1.0》与《连接池方案与多数据源 SQL 血缘解析设计》的**血缘部分**。本文聚焦血缘模块后端：富语义边模型、读/写路径、权限与域过滤、`/api/lineage` 端点契约，并给出 P0-P3 落地状态。前端 UI 见 [../ui/lineage.md](../ui/lineage.md)，使用指南见 [../zh/lineage.md](../zh/lineage.md)。

---

## 1. 现状盘点与设计原则

### 1.1 现状基准

| 维度 | 现状 |
|---|---|
| 后端 | `gov/lineage/LineageService` + `dev/workflow/WorkflowLineageService`；全量 `findAll()` 内存建图；图遍历/BFS 寻径/三色环检测/热度/搜索 | 功能齐全但整图装载、无分层分页，大图 OOM 风险 |
| 数据 | `temporedata_lineage`（物化边/度）、`temporedata_workflow_lineage`（任务级 READ/WRITE）、`temporedata_column_lineage`（列级）、`temporedata_meta_table`（节点） | 表级+列级已有，但列级为"位次映射"，无 `function(fromColumns)→toColumn` 富语义 |
| 解析 | `DruidSqlParser` + `SqlLineageParser` + `SqlDialectRegistry`（方言路由） | 列级支持 JOIN/限定列 |
| 权限 | 无血缘专项权限 | 需补 VIEW/EDIT 血缘与域/租户过滤 |

### 1.2 设计原则（贴合 TemporeData）

1. **不引入专用图库/ES 硬依赖**：默认"关系表 + 内存分层 BFS"；`ES/OpenSearch` 为可选扩展（大数据量/多实例）。
2. **Schema-like 单源契约**：一套 Java 契约（`org.temporedata.api.gov.lineage`）作为后端/前端类型源头（统一 dto + ts 对照）。
3. **富语义边**：每条边携带 `LineageDetails`（SQL、管道、列级 `function`、来源、审计、临时表路径）。
4. **双读路径**：短深精确组装（深度≤3，JDBC）+ 大图探索（分层分页，策略分派 small/medium/large/streaming）。
5. **兼容存量**：血链表 `temporedata_*` 命名，由旧 `zy_*` 经 `V31` 迁移/重命名保留存量；保留现有 `/api/lineage` 端点语义。
6. **权限与域**：读写分别钳制 VIEW/EDIT 血缘；按租户/域返回前过滤并统计隐藏数。

---

## 2. 数据模型（契约先行）

```
EntityLineage          # 血缘查询结果（根实体视图）
  entity               # 根实体引用(type,id,fqn,name)
  nodes[] / upstreamEdges[] / downstreamEdges[]

LineageEdge             # 边
  id / from / to(type,id,fqn)
  lineageDetails?       # 富语义详情

LineageDetails          # 边附加信息
  sqlQuery / columnsLineage[]{fromColumns[], toColumn, function}
  pipeline / description / source / audit(createdBy/At, updatedBy/At)
  tempLineageTables[]   # 中间/临时表逐跳路径

Source enum: MANUAL, VIEW, QUERY, PIPELINE, DBT, SPARK, OPENLINEAGE,
            EXTERNAL_TABLE, CROSS_DATABASE, DASHBOARD
```

### 存储映射（`V31__lineage_enhance.sql`）

- 重命名/迁移：`zy_lineage`→`temporedata_lineage`、`zy_column_lineage`→`temporedata_column_lineage`、`zy_workflow_lineage`→`temporedata_workflow_lineage`、`zy_meta_table`→`temporedata_meta_table`（无旧表则 `CREATE TABLE`）。
- `temporedata_lineage` 增强：`lineage_details_json`（LONGTEXT）、`source`、`created_by/updated_by/created_at/updated_at`（旧行回填）。
- `temporedata_column_lineage` 增强：`from_columns_json`、`function`、`source`、`pipeline_node_id`。
- 关系语义：`from_id →(UPSTREAM)→ to_id`。

### 缓存与配置（`temporedata.lineage`）

```
upstream-depth: 2 / downstream-depth: 2 / layer-size: 50
graph-cache-max: 1000   (Caffeine)
strategy-threshold: {small: 2000, medium: 10000}
time-window-enabled: true
engine: (默认内存; 可选 es)
```

---

## 3. 后端设计

包：`org.temporedata.api.gov.lineage`（契约）+ `org.temporedata.modules.gov.lineage`（实现）。

关键实现类：

| 组件 | 职责 |
|---|---|
| `LineageAssembler` | 精确组装：JDBC 深 ≤3 遍历 `temporedata_lineage` + 实体表，返回 `EntityLineage` |
| `LineageGraphExplorer` | 大图探索：内存分层 BFS + `layerFrom/layerSize` 分页 |
| `LineageStrategySelector` | 按节点数策略分派 `{SMALL,MEDIUM,LARGE,STREAMING}`，防 OOM |
| `LineageWriteGate` | 写并发锁：`DistributedLockProvider`（`td:lock:lineage:{from}:{to}`） |
| `LineageGraphCache` | 专用 Caffeine 图缓存；`invalidateEdge(from,to)` 精准失效 |
| `LineageAuthHelper` | 读取 `SecurityContextHolder` 判 superAdmin + `PermissionService.verify("LINEAGE","READ|WRITE")` |
| `LineageDomainFilter` | 按租户/域过滤返回 nodes/edges，`hidden=true` + `hiddenCount` |
| `LineageProperties` | 绑定 `temporedata.lineage.*` 配置 |

> 组装编排放独立 `LineageExplorationService`，避免 `LineageService` 膨胀。

### 3.1 读路径（双形态并存）

| 形态 | 实现 | 场景 |
|---|---|---|
| 精确组装 | `LineageAssembler` | 实体详情页 / 受控深度（≤3） |
| 图探索 | `LineageGraphExplorer` | 大图 / 探索页（分层分页） |

`LineageGraphExplorer` 要点：策略分派、分层遍历（FQN 集合聚合、深度递减）、方向键哈希 `hash(fqn)`、`LineagePathPreserver` 保路径、`LineageProgressTracker` 长遍历进度、**时间窗硬剪枝**（边 `createdAt/updatedAt` 范围重叠判定，越权边切断可达节点；旧无时间戳边恒命中）。

### 3.2 写路径

1. 校验 from/to/pipeline 引用（NON_DELETED）+ 两端权限（`authorizeLineageReference`）。
2. 列级合法性校验（`function(fromColumns[])->toColumn`，正则校验 FQN 格式）。
3. 写入 `temporedata_lineage`（关系 + `lineage_details_json`）+ `temporedata_column_lineage`。
4. 缓存失效：`invalidateEdge(from,to)`，其余 TTL 兜底。
5. 变更推送：WebSocket `ChangeEvent`（复用平台消息通道）。
6. 扩展血缘：来源为 PIPELINE 的边自动补 Workflow/Domain/DataProduct 级边。

### 3.3 并发安全与事务

- 写入 `@Transactional`；`DistributedLockProvider` 防并发重复边。
- Caffeine 图缓存设上限，不缓存超阈值大图，直接流式探索。

---

## 4. REST API 契约（`/api/lineage`）

### 读取（新增，保留现有 `/graph /overview /impact /lineage /path /cycles /heat /search /export` 为兼容别名）

| 端点 | 说明 |
|---|---|
| `GET /{entityType}/{id}?upDepth&downDepth` | 按 ID 精确组装（≤3 深） |
| `GET /{entityType}/name/{fqn}?upDepth&downDepth` | 按 FQN 精确组装 |
| `GET /explore?fqn&direction&layerFrom&layerSize&queryFilter&columnFilter&timeFrom&timeTo` | 大图探索（分层分页）→ `SearchLineageResult{nodes,edges,totalNodes,totalEdges,hiddenCount}` |
| `GET /explore/{direction}` | 定向探索 |
| `POST /hydrate` | 批量水合：一次补全多节点实体，避免 N 次 GET |
| `GET /edge/{from}/{to}` | 单边详情（LineageDetails） |
| `GET /dataquality` | 质量血缘（叠加质量规则节点） |
| `GET /export?format` | CSV 导出（同步/异步任务） |

### 写入（权限 `VIEW_*/EDIT_LINEAGE`，边两端分别鉴权）

| 端点 | 说明 |
|---|---|
| `PUT /edge/{from}/{to}` | 新增边（LineageDetails 可选） |
| `PATCH /edge/{from}/{to}` | 部分更新边（列级/SQL/描述） |
| `DELETE /edge/{from}/{to}` | 删除边 |
| `DELETE /type/{source}` | 按来源批量删除 |
| `POST /refresh` | 主动重建（`rebuild` 兼容） |

### 图数据来源（`/graph`）

`GET /graph?nodeId=` 从 `temporedata_lineage` 读取（由 `POST /rebuild` 生成）；`POST /rebuild` 从 `temporedata_workflow_lineage`（任务级 READ/WRITE 边）+ `temporedata_meta_table`（孤立表节点）构建。

---

## 5. 采集端与血缘来源注入

- **SQL 血缘解析**：继续用 `DruidSqlParser`/`SqlLineageParser`，输出增强为 `function(fromColumns[])->toColumn`；`WorkflowLineageService.sync` 落库写 `source=PIPELINE`、`pipeline=node`、`sqlQuery`。
- **元数据采集联动**：`MetaCollector`/`CatalogAssetPostProcessor` 采集的 VIEW 生成 `source=VIEW`，跨库引用 `source=CROSS_DATABASE`。
- **手动**：前端编辑连线 `source=MANUAL`。
- 平台侧保留 `source` 枚举过滤、按来源删除、审计展示。

---

## 6. 前端承接

- 栈：Vue3 + AntV G6（只读渲染）+ ECharts；编辑/探索 API 通过接口级契约调用，**不在画布内直接编辑**。
- `api/modules/lineage.js` 封装：`getEntity/getEntityByFqn/explore/exploreDir/hydrate/getEdge/getDataQuality/saveEdge/patchEdge/deleteEdge/deleteBySource/exportLineage/rebuild/getGraph/...`。
- `stores/lineage.js` + `views/LineageV2.vue`（4 区块布局）+ 组件化（详见 [../ui/lineage.md](../ui/lineage.md)）。

---

## 7. 分阶段落地（状态）

| 阶段 | 交付 | 状态 |
|---|---|---|
| P0 | 契约类型 + `temporedata_*` 血缘增强迁移 + 富语义读写 + 精确组装 + 权限(VIEW/EDIT) + 域过滤骨架 | ✅ |
| P1 | 图探索引擎（分层分页 + 策略 + 时间窗硬剪枝 + hydrate + CSV 导出） | ✅ |
| P2 | 前端（原计划 VueFlow 编辑模式，**已按决定移除**；当前以 G6 只读为准，编辑 API 保留供有权限客户端调用） | ✅（G6 只读 + 后端编辑 API 保留） |
| P3 | 来源注入全链路（VIEW/PIPELINE/MANUAL/CROSS_DB/DBT/SPARK）+ 变更推送 +（可选）ES 图索引 | 部分 |

**验收**：实体血缘 ≤3 深精确；>threshold 走分层探索不整装载；编辑连线/列级 function 可存可展；按 source 过滤与删除；域/租户过滤隐藏节点并报告数量；前端大图不卡顿（Canvas + 分层）。

> **P2 关键决策**：数据血缘编辑模式（VueFlow）最终**未落地**——`LineageFlowCanvas/LineageNode/LineageLayers/LineageControls` 组件与 store 编辑态已删除；当前为纯 G6 只读。后端编辑/探索 API（edge 写、explore/hydrate/dataquality/export）全部保留，前端仅通过接口级契约调用，不引入 elkjs/HTML2canvas 依赖。

---

## 8. 兼容与回归

- 保留 `/api/lineage/graph|overview|impact|lineage|path|cycles|heat|search|export` 为兼容别名。
- `temporedata_lineage` 增强字段给默认值，存量由 `V31` 从旧 `zy_*` 迁移/重命名，无需停机。
- 图探索默认内存，仅 `temporedata.lineage.engine=es` 且提供 ES 时走图索引。
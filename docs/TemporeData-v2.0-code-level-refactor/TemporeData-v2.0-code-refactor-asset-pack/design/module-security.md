# 数据访问控制与动态脱敏执行链设计（Security）

## 1. 背景与目标

系统具备安全规则与授权的**管理面**，但原先"查询 / DataApi / 导出"三条出口是模拟/占位执行，未形成执行链。目标：建立统一 **"数据访问控制 + 动态脱敏" 执行引擎**，在查询、数据服务（DataApi）、导出三条出口强制生效——"谁能看哪些数据的哪些列/哪些行、看多少、是否脱敏"。**核心约束：大数据集下仍保持有效**（避免全量装载、应用侧逐行脱敏、可水平扩展）。

---

## 2. 设计原则（大数据量下的立论前提）

> **"既杀又扫"是失效根源。** 在大数据集上用「全量拉取 → 应用过滤 → 应用脱敏」会带来传输/内存爆炸、逐行函数开销、无法扩展。本设计把"裁剪、过滤、脱敏"尽量**下推**到数据源执行，应用侧只做无法下推的兜底且一律**流式**。

- **下推优先**：能改 SQL 就改（列裁剪、权限谓词、脱敏表达式），让数据库/引擎干重活。
- **应用兜底流式**：只能应用处理的（复杂正则、方言不支持下推），流式 ResultSet 分批，绝不整集加载。
- **决策与执行分离**：授权决策（`AccessPolicyResolver`）轻、可缓存、无状态；执行（SQL 改写 + 结果处理）走现有查询器，可横向扩展。
- **失效兜底（Fail-closed）**：策略不确定/解析失败默认拒绝或按最严（隐藏列 + 全文脱敏）。

---

## 3. 总体架构：GovernedSqlExecutor（执行引擎）

```
              ┌─────────────── 三条出口 ───────────────┐
 QueryService  DataApiService        导出/下载
        │            │                    │
        └────────────▼────────────────────┘
              GovernedSqlExecutor (统一治理执行引擎)
                        │
   ┌────────────────────┼────────────────────┐
   ▼                    ▼                    ▼
 AccessPolicyResolver SqlPlanRewriter  DataAccessGovernor/DataDesensitizer
 (决策层·无状态)       (下推层·Stage B)    (兜底层·Stage A·流式)
   │                    │                    │
 PolicyCache(Redis)   SQL 片段           ResultStream
   +委托 RBAC/MyData ─→DB 执行→
```

**三阶段管道**（每个请求）：

1. **授权决策** `AccessPolicyResolver`：输入 principal(user, tenant) + 访问对象(datasourceId/table/column) + 动作；综合 RBAC 资源授权、`MyData` 资产授权、数据分级/分类；产出 `AccessPolicy{grantedColumns, rowFilter, desensitizeRules, denied}`；**无状态 + 可缓存**（同一 `(user, resource)` 结果缓存，授权事件驱动失效）。
2. **下推** `SqlPlanRewriter`：列裁剪（`SELECT *` → 授权列清单）；行级行权谓词注入 `AND <rowFilter>`；脱敏下推（数据源支持时改写 `CONCAT(SUBSTR(col,1,3),'****')`、`MD5(col)`）；交 `SqlFirewall` 复核改写后 SQL。
3. **执行 + 兜底** `DataAccessGovernor`：执行请求到数据源（`fetchSize` 流式读）；仅对"无法下推脱敏"的列在流式结果上逐行变换。

---

## 4. 大数据量有效性（核心节）

1. **列裁剪前置（成本最低收益最大）**：`SELECT *` 是大数据集头号浪费，只下推授权列，未授权列不出现；不做"先查全表再删列"。
2. **行级权限谓词下推（RLS）**：`rowFilter` 以可下推谓词注入 WHERE，由数据库过滤，DB 索引 + 谓词下推自动生效；不适于下推的复杂权限回退结果流过滤且限制小结果集/强制分页。
3. **脱敏下推优先**：数据源/方言支持则改写为原生表达式（在数据源完成脱敏，不占应用 CPU、可随数据源分片扩展）；无法下推只落到少量窄列，单独构建小计划。
4. **应用兜底一律流式**：`fetchSize` 分批、流式迭代；导出/DataApi 大结果用分页/流响应，单批上限；脱敏函数 O(1)/O(n) 预编译 Pattern。
5. **决策层高性能**：`AccessPolicy` 由授权事件失效 + 本地/Redis 两级缓存；`PolicyResolver` 无状态可水平扩展。
6. **异步审计不阻塞主链路**：每次放行/拦截/脱敏决策异步写审计事件（队列/内存缓冲 + 批量落库）。
7. **性能预算与熔断**：下推脱敏覆盖率目标 ≥90%；超大结果强制分页或拒绝；`MAX_RESULT_ROWS`、`APP_MASK_BUDGET` 熔断。

---

## 5. 脱敏器（DataDesensitizer）

基于 `MaskRule.ruleType + maskPattern`，映射固定函数集合（预编译、O(n)）：

| ruleType | 行为 |
|---|---|
| PHONE / NAME / BANK_CARD | 保留头尾，中段 `***`（可下推 `CONCAT/SUBSTR`） |
| ID_CARD | 保留前 6 + 后 4，中段 *** |
| EMAIL | 保留域名 + 首字符 |
| CUSTOM | `maskPattern` 即替换串（可正则） |
| HASH | 可选 `MD5/SHA256(col)`（弱匿名化，一致性关联） |
| DROP | 置 `null`（最高泄露风险列） |

---

## 6. 数据模型与迁移

**新增策略表（Flyway V22）`zy_access_policy`**：

```
id, principal_type(USER/ROLE), principal_id,
resource_type(DS/TABLE/COLUMN), resource_id, resource_name(表.列),
grant_type(GRANT/DENY), access_type(READ/EXPORT/API),
row_filter(可下推谓词, nullable),
mask_rule_id(引用 MaskRule, nullable),
expire_at, tenant_id, created_at
```

- 与 `zy_mydata`（资产授权）关系：`MyData` 作为 `grant_type=GRANT` 的已有授权来源；本表为**统一判定物**（决策层读两者并集/交集）。
- 与 `zy_data_level`/`DataCategory`/`zy_mask_rule`：分级/分类 → 推导默认脱敏级别；`MaskRule` 提供具体脱敏函数。
- 缓存失效：本表变更 → Redis 发布/订阅通知失效对应缓存键。

---

## 7. 分阶段落地（状态与实现细节）

| 阶段 | 交付 | 大数据量要点 |
|---|---|---|
| P0 | 引擎骨架 `AccessPolicy` + 结果层流式脱敏 + 列裁剪 | §4.1/§4.4 |
| P1 | `SqlPlanRewriter`：列裁剪 + `MaskRule` 下推 + 行级谓词注入 | §4.2/§4.3 |
| P2 | 接 DataApi + 导出流 + 调用方认证/签名/限流 | §4.4 |
| P3 | 策略缓存两级 + 授权事件失效 + 异步审计 + 熔断 | §4.5/§4.6/§4.7 |

### 已落地状态核验

- ✅ **P0**：`GovernedSqlExecutor`、`DataAccessGovernor`（结果层列裁剪 + 逐行脱敏）、`DataDesensitizer`（PHONE/NAME/EMAIL/ID_CARD/BANK_CARD/CUSTOM/HASH/DROP）、`AccessPolicyResolver`（读 `zy_mask_rule` 自动生成策略）、`MaskRule` 端点 `/api/security/mask-rule`。
- ✅ **P0 接入通用查询**：`QueryService.execute`（`/api/query/execute`）改为真实 SELECT 执行 + 治理——加载注册数据源 → `JdbcSupport.query` → `SqlLineageParser` 推导被查表 → `AccessPolicyResolver` 解析脱敏规则 → `GovernedSqlExecutor` 列裁剪 + 逐行脱敏；结果持久化 `zy_query.columns_json/rows_json`（`V27` 建表）回传前端。
- ✅ **P1 SQL 下推**：`SqlPlanRewriter` 实现 MySQL 族方言脱敏下推（`CONCAT/SUBSTRING/MD5`），`GovernedSqlExecutor` Stage B 下推（失败回退 Stage A 应用侧流式治理）；`SELECT *` 含规则下 fail-closed 回退，避免明文泄露。
- ✅ **P2 DataApi / 导出 / 调用方认证**：`DataApiService.invoke`（`POST /api/services/{id}/invoke`）`X-API-Key + X-Timestamp + X-Nonce + HMAC-SHA256(apiKey, ts:method:path:nonce)` 签名（±5min 重放窗）+ 按 apiKey 固定窗口限流（`temporedata.dataapi.rate-limit`，默认 60/min）+ 治理引擎执行；`GET /api/query/export` 治理后 CSV 流式导出（`StreamingResponseBody`）；`test()` 真实验证。
- ✅ **P3 缓存/失效/审计/熔断**：`AccessPolicyResolver.resolve` `@Cacheable("security_rule")`（Caffeine L1 + Redis L2），`MaskRuleService` 写操作 `@CacheEvict` 驱动失效；`GovernanceAudit.auditAsync` `@Async("governanceAuditExecutor")` 单线程队列写防篡改审计链，每次放行/拒绝/失败异步落 `DATA_ACCESS` 事件；熔断 `MAX_RESULT_ROWS=500`（`truncated` 标志）、`APP_MASK_BUDGET=5`。
- ✅ **SQL 防火墙接入**：`SqlFirewall`（Druid Wall，方言感知）接入 `GovernedSqlExecutor`，改写后/原始 SQL 均在与数据源交互前过防火墙复核（fail-closed：`multi-statement` 违规直接拒绝并异步落 `DATA_ACCESS/DENY` 审计）。

> **类名订正**：设计期命名 `DynamicDesensitizer`/`PolicyCache`/`SqlPlanRewriter` → 实际实现为 `DataDesensitizer`（脱敏函数）、`DataAccessGovernor`（执行器/Stage A 列裁剪）、`AccessPolicyResolver`（策略解析），`SqlPlanRewriter`（Stage B 下推）已实现。以实际类名为准。

---

## 8. 涉及文件

- **新增（service `gov/security`）**：`GovernedSqlExecutor`、`AccessPolicy`、`AccessPolicyResolver`、`SqlPlanRewriter`、`DataAccessGovernor`、`DataDesensitizer`、`GovernanceAudit`。
- **接入**：`QueryService`、`DataApiService`（执行时）、导出/下载链路（流式）。
- **复用**：`PermissionService`（RBAC）、`MyData`、`MaskRule`、`SqlFirewall`、`CryptoUtil`、Redis、审计链。
- **迁移**：V22 建 `zy_access_policy`；V27 建 `zy_query` 治理结果表。

---

## 9. 风险与边界

- 方言差异：脱敏下推仅在受支持方言启用，其余走兜底，避免"下推失败即阻塞"。
- 性能：决策缓存命中热路径 <1ms；脱敏兜底线批量与并行上限。
- 安全：**fail-closed**（策略缺失→拒/最严）；改写后 SQL 必过防火墙。
- 非目标：KMS 密钥托管、传输 TLS 强制、静态加密、合规报表（归入网络/基础设施后续）。

> **结论**：把安全执行从"全量拉取后应用处理"改为"列裁剪 + 权限谓词 + 脱敏下推到数据源，应用仅流式兜底"，从机制上保证亿行/大分区数据集下安全不成为性能瓶颈，且可随查询器水平扩展。
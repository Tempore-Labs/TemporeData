# SQL 解析器设计（SQL Parser）

> 归并原两篇：《temporedata-SQL解析器与Druid定位设计》、《temporedata-单一SQL解析器Druid化重构设计》。本文回答：① SQL 解析器的设计是什么；② "Druid 仅作 SQL 解析器"是否可行；③ 已落地**单一 Druid 解析器**重构的现状。

---

## 1. 背景与澄清

- **Swagger 真实存在**：系统已基于 springdoc（OpenAPI 3）提供 `/swagger-ui.html`、`/v3/api-docs`，为既定事实，保留。
- **Druid 定位澄清**：`/druid` 实际是 **Druid 连接池的 Web 监控控制台**（`stat-view-servlet`），**并非 SQL 解析器**。
- 现状：SQL 解析曾由 JSqlParser + Druid 双实现驱动，后演进为**单一 Druid 解析器**（详见 §4）。

---

## 2. SQL 解析抽象（保留的契约）

平台为解析能力保留统一抽象层，便于未来扩展（如元数据精确解析）：

```
SqlParser (* interface*)
  └─ SqlParseResult parse(String sql, SqlParseContext ctx)
       ├─ success / message / sqlType
       ├─ targetTable / sources / tables        （血缘 / 元数据用）
       ├─ columns / columnLineage               （列级血缘）
       ├─ dialectInfo                            （方言、可选）
       └─ diagnostic: { valid, risk }            （校验 / 防火墙，可选）

实现（当前唯一）：
  DruidSqlParser   （单一实现，supports 全方言，基于 SchemaStatVisitor 提取血缘）
```

能力分层：

| 能力 | 说明 | 提供方 |
|---|---|---|
| 血缘提取 | 表级 target/source + 列级（JOIN/限定列） | Druid（`SchemaStatVisitor`） |
| 方言扩展 | MySQL/PG/Oracle/ClickHouse/SQLServer/H2/DB2… | Druid `DbType` |
| SQL 注入防护 | WallFilter 防火墙 | Druid Wall（`SqlFirewall`） |

`SqlLineageParser` 为对 `SqlParser` 的桥接，对外 `SqlParseRes` 字段保持兼容（前端 / controller 不变）。

---

## 3. "Druid 仅作 SQL 解析器"可行性评估

**结论：可行 ✅**，但需满足前提：
- 引入**解析库**（`com.alibaba:druid` 核心包）而非 `druid-spring-boot-starter`（连接池 + 监控）。
- 连接池职责移交 HikariCP（见 [module-datasource.md](module-datasource.md)），移除 `/druid` 控制台。
- 仅在确需更广方言 / 语法校验 / 防火墙时引入。

权衡：JSqlParser 轻、Apache 友好、血缘提取直接；Druid 方言覆盖广（含 ClickHouse）、自带 WallFilter、但体积重。**最终统一到 Druid**（见下）。

---

## 4. 单一 Druid 解析器重构（已落地）

### 4.1 为什么从"两个解析器"收敛为一个

原有两个解析器靠 `SqlParserRegistry` 按方言路由：MySQL/PG/Oracle/Doris/StarRocks 走 JSqlParser，ClickHouse 走 Druid。这是**渐进式引入的历史产物**，代价是两套逻辑、行为可能分叉、维护翻倍。而 Druid 1.2.16 本身已支撑多方言（6 种数据源全覆盖），因此统一为单一解析器。

### 4.2 原则

- **保留** `SqlParser` 接口 + `SqlParserRegistry`。
- **收敛实现**：`DruidSqlParser` 升为唯一实现，`supports(一切方言)=true`；**删除** `JsqlParserSqlParser` 与 JSqlParser 依赖。
- **统一入口**：`parse(sql, dialect)` 经 `SQLParserUtils.createSQLStatementParser(sql, dbType)` 解析，再走统一提取逻辑。

### 4.3 方言 → DbType 映射（单点）

| Dialect | DbType |
|---|---|
| MYSQL / DORIS / STARROCKS / OTHER | mysql |
| POSTGRESQL | postgresql |
| ORACLE | oracle |
| CLICKHOUSE | clickhouse |
| HIVE / OCEANBASE | mysql（近似解析） |

> 未知方言 `OTHER` 回退 `mysql`，解析失败 `success=false` 不抛（沿用降级契约）。

### 4.4 血缘提取改为基于 SchemaStatVisitor

```
parse(sql, dialect)
  ├─ SQLStatement stmt = SQLParserUtils.createSQLStatementParser(sql, dbType).parseStatement();
  ├─ SchemaStatVisitor visitor = new SchemaStatVisitor(); stmt.accept(visitor);
  ├─ targetTable   ← Insert/Update/Delete/Create 的目标表
  ├─ sources       ← 读取表（去掉 target）
  ├─ tables        ← 全部表
  ├─ columns       ← 列引用
  ├─ columnLineage ← Insert→Select 列映射（含 JOIN 多源 / 限定引用 alias.col 归属）
  └─ sqlType       ← 语句类型
```

目标：`SqlParseResult` 字段契约**不变**（`success/sqlType/targetTable/sources/tables/columns/columnLineage/dialect`），前端与 `SqlLineageParser` 桥接零改动。

### 4.5 自动化方言扩展：新增数据源零代码改动

`SqlDialectRegistry`（放 `temporedata-api.datasource.sql`）作为唯一方言知识源（`Map<Dialect, DbType>`），`DruidSqlParser` 与 `SqlFirewall` 都从它取 `DbType`，消除重复 switch：

```
SqlDialectRegistry
   ├─ 内置条目：MYSQL→mysql、POSTGRESQL→postgresql、ORACLE→oracle、
   │           CLICKHOUSE→clickhouse、DORIS→mysql、STARROCKS→mysql
   ├─ SPI 追加：每个数据源插件（META-INF/services/...SqlDialectContribution）
   │           声明 DatasourceType 对应 DbType（或解析 Hook）→ register()
   └─ 提供：dbType(Dialect)、exists(Dialect)、providers()

DruidSqlParser（单一实现）
   ├─ supports(d)      = sqlDialectRegistry.exists(d)
   ├─ parse(sql, d)    → createSQLStatementParser(sql, sqlDialectRegistry.dbType(d))
   └─ 血缘            统一走 SchemaStatVisitor 提取
```

**自动化层级（L0→L3）**：

| 层级 | 能力 | 新增数据源动作 |
|---|---|---|
| L0 | Druid 内建多方言，单解析器按 DbType | 无 |
| L1 | `Dialect→DbType` 注册表 | 加/注入一条映射（配置或 SPI） |
| L2 | 与插件 SPI（`DatasourceType`）打通 | 插件自带 `DbType` 声明，启动自动 register → **零代码** |
| L3 | 未知方言 fallback | 近似 mysql + 标记 `dialect=OTHER`，降级 `success=false` |

> 因此"每增加一个数据源就重写 DruidSqlParser"不成立：新增源 = 新增一条注册，默认即可自动解析与血缘。

---

## 5. 列级血缘增强（已落地，P3）

`DruidSqlParser.collectColumnLineage` 由"仅单源 `INSERT…SELECT` 位置映射"升级为支持 **INNER/LEFT JOIN 多源**与**限定引用（`alias.col`）**：

- 递归遍历 FROM/JOIN 建立「别名→物理表」映射；`f.account` / `o.region` 精确归属到各自源表；函数 / 子查询 / 字面量不误归因。
- 覆盖 `INSERT…T 列 <- 源表.源列` 的列血缘（`SqlParseRes.columnLineage` → workflow `sync` → `zy_column_lineage` → `graph().columnEdges`）。
- 验证示例：`INSERT INTO agg_t (acct,region) SELECT f.account,o.region FROM fund_flow_stats f LEFT JOIN org_t o ON f.org_id=o.id` → `acct←fund_flow_stats.account`、`region←org_t.region`。单测 `DruidSqlParserLineageTest` 2/2 通过。

---

## 6. 涉及文件

- 修改：`temporedata-service` — `DruidSqlParser`（统一实现）；删除 `JsqlParserSqlParser`。
- 修改：`temporedata-service/pom.xml`、`temporedata-bom/pom.xml`（去 jsqlparser）。
- 新增：`temporedata-api.datasource.sql` — `SqlDialectRegistry` + `SqlDialectContribution`（SPI）。
- 不改：`SqlParser`/`SqlParseResult`/`Dialect`/`SqlParserRegistry`、`SqlLineageParser` 桥接、controllers、前端契约。

---

## 7. 风险与权衡

- 切到 Druid 在方言解析与血缘提取细节上与 JSqlParser 有差异，需回归核心 DML/DDL。
- 保留 abstraction 的价值：未来结合元数据做精确列血缘时，可新增 `MetaSqlParser` 而不破坏契约。

## 8. 非目标

- 不改 `SqlParser` 接口、`SqlParseResult` 字段、前端 DAG 展示契约。
- 不引入元数据驱动的精确列血缘（可拆后续专项）。
- 不引入 Calcite 作为运行时解析层（BOM 中仅 provided 授权态）。
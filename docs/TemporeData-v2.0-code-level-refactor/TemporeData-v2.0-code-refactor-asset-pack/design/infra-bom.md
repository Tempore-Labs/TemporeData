# BOM 统一依赖管理设计（Bill of Materials）

## 1. 背景与问题

temporedata 是 Maven 多模块工程，当前第三方依赖版本号**分散硬编码在 4 处**，导致升级失控、重复且不一致：

| 位置 | 承载版本 | 典型问题 |
|---|---|---|
| 根 `pom.xml` `<properties>` | 引擎 spark/flink/hadoop/calcite、maven-resolver、lombok | 引擎版本与本工程版本耦合在聚合 POM |
| `temporedata-service/pom.xml` | druid/flyway/springdoc/jgrapht/jsqlparser/hutool/... | 硬编码版本最多，最易失控 |
| `temporedata-server/pom.xml` | flyway/springdoc | **与 service 重复**，随时可能不一致 |
| 数据源插件各 `pom.xml` | clickhouse-jdbc 0.4.6 / ojdbc11 21.9.0.0 | 版本散落插件目录 |

具体痛点：① 同一依赖多处声明（`flyway-core 8.5.13`、`springdoc-openapi-ui 1.7.0`）升级需同步改多处；② 自定义三方版本全部硬编码无单一管理；③ 无版本治理规约；④ 部署口径不统一，缺少可整体 import 的载体。

---

## 2. 目标

1. **单一出处**：新建独立 `temporedata-bom` 模块作为**全工程第三方依赖版本唯一出口**。
2. **去版本号**：各业务模块只声明 `groupId/artifactId`，**不写 `version`**。
3. **基线统一**：以 `spring-boot-dependencies 2.7.18` 为基底（import），自定义/覆盖版本集中在 BOM。
4. **部署友好**：BOM 提供"统一依赖清单"，装配/部署方一次性引入。
5. **兼容现有结构**：保留 `temporedata-datasource-plugin` 聚合父方案，BOM 只做版本管理。

---

## 3. 方案总体结构

```
temporedata（根聚合 pom，packaging=pom）
├── temporedata-bom                    ← 新增：版本唯一出口（dependencyManagement + import Spring Boot）
├── temporedata-common / -api / -support / -security
├── temporedata-service                ← 引用 BOM，去版本
├── temporedata-datasource-plugin      ← 聚合父（数据源插件），插件引用 BOM
└── temporedata-server                 ← 引用 BOM，去版本
```

要点：`temporedata-bom` 是 `packaging=pom` 的纯版本契约模块，不含业务代码；通过 `<dependencyManagement><scope>import</scope>` 引入 `spring-boot-dependencies`，再追加/覆盖平台自有版本。消费模块在自身 `<dependencyManagement>` 中 `import` 该 BOM。根聚合 POM 只做模块聚合，不再承担版本管理。

---

## 4. 依赖分组清单（统一后）

各分组均声明在 BOM 的 `dependencyManagement`，版本放 `<properties>`。

**（A）数据源驱动组**

| groupId:artifactId | 版本 |
|---|---|
| com.mysql:mysql-connector-j | 8.0.33 |
| org.postgresql:postgresql | 42.5.4 |
| com.clickhouse:clickhouse-jdbc（classifier=all） | 0.4.6 |
| com.oracle.database.jdbc:ojdbc11 | 21.9.0.0 |
| com.alibaba:druid | 1.2.16 |

**（B）数据库迁移组**

| groupId:artifactId | 版本 |
|---|---|
| org.flywaydb:flyway-core / flyway-mysql | 8.5.13 |

**（C）引擎组（provided，打包默认不内嵌）**

| groupId:artifactId | 版本 |
|---|---|
| org.apache.spark:spark-core_2.12 / spark-sql_2.12 | 3.4.1 |
| org.apache.flink:flink-clients / flink-table-api-java | 1.18.1 |
| org.apache.hadoop:hadoop-client | 3.3.5 |
| org.apache.calcite:calcite-core | 1.27.0 |

**（D）运行/解析/编排组**：`com.github.jsqlparser:jsqlparser 4.8`、`org.jgrapht:jgrapht-core 1.4.0`。

**（E）云存储/压缩组**：`software.amazon.awssdk:s3 2.21.45`、`com.github.luben:zstd-jni 1.5.5-11`。

**（F）工具库组**

| groupId:artifactId | 版本 |
|---|---|
| cn.hutool:hutool-all | 5.8.16 |
| com.cronutils:cron-utils | 9.2.1 |
| com.jcraft:jsch | 0.1.55 |
| org.apache.poi:poi-ooxml | 5.2.5 |
| org.mapstruct:mapstruct / mapstruct-processor（provided） | 1.5.3.Final |

**（G）Web/API 文档组**：`org.springdoc:springdoc-openapi-ui 1.7.0`。

**（H）安全组**：`io.jsonwebtoken:jjwt-api/impl/jackson 0.11.5`。

**（I）依赖解析基础设施组**：`org.apache.maven.resolver:（api/impl/connector-basic/transport-http/supplier）1.9.22`。

**（J）基础编译依赖**：`org.projectlombok:lombok（provided）1.18.30`。

> 由 `spring-boot-dependencies` 已管理且平台不覆盖的依赖（各 `spring-boot-starter-*`、jackson、caffeine 等）无需重复声明。

> ⚠️ 注：`jsqlparser` 已在《单一 SQL 解析器 Druid 化重构》中移除，随实现从 BOM 中删除；此处保留分组历史以说明依赖治理全景。

---

## 5. 各模块如何接入 BOM

**`temporedata-service`（消费方示例）**：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.temporedata</groupId><artifactId>temporedata-bom</artifactId>
            <version>${project.version}</version><type>pom</type><scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
<dependencies>
    <dependency><groupId>com.alibaba</groupId><artifactId>druid</artifactId></dependency>
    <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-core</artifactId></dependency>
    <!-- 不再写 <version> -->
</dependencies>
```

**数据源插件**：各插件 POM import BOM 后，驱动 `<version>`/本地 `<properties>` 一律移除（如 `clickhouse-jdbc` 仅留 `<classifier>all</classifier>`）。

**根聚合 POM**：`<modules>` 增加 `temporedata-bom`；移除 `<properties>` 中引擎/工具版本（平移进 BOM），仅保留模块级构建属性；移除根 `dependencyManagement` 中 jjwt 版本。

---

## 6. 实际部署："统一添加执行模块"机制

> 部署阶段需把"驱动 + 引擎 + 执行相关依赖"作为**统一整体**引入，保证运行时版本与开发一致。BOM 本身只管理版本、不引入依赖，提供三层落地（可叠加）：

1. **导入统一版本契约**：`temporedata-server`（fat jar 装配模块）import BOM，保证最终可执行产物内所有依赖版本唯一、可审计。
2. **启动装配 profile**：`-Pinclude-engine-spark/-Pinclude-engine-flink` 把 provided 引擎转 compile 打入 jar，适合"一体化部署、不单独拉起引擎"；默认内嵌引擎不打，体积最小。
3. **部署方整体引用**：外部工程 `<dependencyManagement>` 中 `import org.temporedata:temporedata-bom`，获得与平台一致的驱动与依赖版本清单（driver consistent across deployment）。

---

## 7. 迁移步骤与构建约束

1. 新建 `temporedata-bom/pom.xml`。
2. 根 `pom.xml`：`<modules>` 加 `temporedata-bom`；清理版本类 properties 与 jjwt dependencyManagement。
3. `temporedata-service` / `temporedata-server`：加 BOM import，移除硬编码 `<version>`。
4. 数据源插件：加 BOM import，移除 clickhouse-jdbc / ojdbc11 的 `<version>` 与本地 properties。
5. `mvn -DskipTests install` 全量构建，`mvn dependency:tree` 校验版本收敛、无双版本。
6. 启动应用自检。

> **构建约束（易踩坑）**：多模块内对自身模块使用 `<scope>import</scope>` 的 BOM，**不会从 reactor 解析**，只从本地/远程仓库解析。因此：`temporedata-bom` 须位于根 `<modules>` **首位**，且需执行 **`mvn install`**（非 `package`）先装入本地仓库；产线/派生产品外部引用需先发布 BOM。

---

## 8. 升级与维护规约

- **唯一改点**：升级任何三方依赖只改 BOM 对应 `<property>`，其余模块零改动。
- **重复检查**：CI 增加 `mvn dependency:tree -Dverbose` 防重复 scope 冲突。
- **引擎与驱动分离**：驱动随 BOM 同步；引擎版本在 BOM 引擎组单独维护，不阻塞普通发布。
- **兼容结构不变**：`temporedata-datasource-plugin` 聚合父保持原样。

## 9. 与聚合父方案的关系

结构维度（聚合父 + 插件）与版本维度（BOM）**正交**：聚合父负责"模块怎么组织"，BOM 负责"版本怎么统一"，互不冲突。
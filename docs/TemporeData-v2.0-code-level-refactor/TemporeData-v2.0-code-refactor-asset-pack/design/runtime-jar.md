# 运行模式重设计：开发 / 迭代免重复打 jar（Runtime）

## 1. 痛点与目标

**现状**：日常"运行项目"走生产单 jar 链路——前端 `npm run build` 写入 `temporedata-server/.../static`，`mvn -pl temporedata-server -am package` repackage 成 `temporedata.jar`，再 `java -jar`。于是任何前端/后端改动都要重打 jar → 重启。

**根因**：
1. 前端静态被**内嵌进 jar**（`WebMvcConfig` 只从 `classpath:/static/` 读），前端一改就要重打包。
2. 后端只以 `java -jar` 运行，缺少热重启/增量运行通道。

**目标**：把"开发/迭代的日常运行"与"生产发布"解耦——日常改代码**不再重打 jar**；`mvn package` 仅保留给**发布/镜像**。

---

## 2. 设计原则

- **运行与发布分离**：开发用"源码/类路径"运行（`spring-boot:run` + DevTools），发布才打包。
- **前端与后端解耦**：开发时前端由 Vite（热更新）或**外部静态目录**提供，后端不再内嵌前端。
- **磁盘即服务**：前端构建产物放独立 `dist/`，后端以 `file:` 静态目录按需读取 → 前端 build 后无需重打 jar、无需重启后端。
- **热重载兜底**：后端 Java/配置改动依赖 Spring Boot DevTools 自动重启（秒级）。

---

## 3. 目标运行形态

| 用途 | 前端 | 后端 | 是否重打包 | 变更生效 |
|---|---|---|---|---|
| 日常迭代（推荐） | Vite dev 5174（HMR） | `spring-boot:run` + DevTools（8080） | **否** | 前端即时；后端自动重启 |
| 真实构建 UI 联调 | `npm run build`→dist | `spring-boot:run` 从 `file:dist` 服务前端 | **否** | 前端 rebuild 即加载，后端改动自动重启 |
| 生产/镜像 | CI `npm build` + `mvn package` | jar/镜像 | 是（仅发布时） | 一次性 |

---

## 4. 三个关键改造

### 4.1 后端：`spring-boot:run` + DevTools（免 `java -jar`）
- `temporedata-server` 增加 `spring-boot-devtools`（`runtime`、`optional`，生产 jar 不包含）。
- 日常 `mvn -pl temporedata-server spring-boot:run`（或 IDE 运行 `TemporeDataApplication`），后端以**类路径**运行，不产 fat jar；Java 改动保存后 DevTools 自动重启（不重新打包，秒级）。

### 4.2 前端：构建产物落到独立 `dist/`，后端按需读 `file:`
- `temporedata-ui/vite.config.js` 的 `build.outDir` 从 `../server/.../static` 改为 `./dist`（`emptyOutDir: true`）。
- 改造 `WebMvcConfig.addResourceHandlers`：**优先从外部目录 `file:./temporedata-ui/dist/` 读前端静态**（存在即用），不存在回退 `classpath:/static/`（发行 jar 兼容）；通过 `temporedata.ui.static-dir` 开关 + 环境变量控制。
- 生产发布仍由 CI 把 `dist` 打进 jar 或挂载卷。

### 4.3 一键运行器：根 `Makefile` / `bin/dev.sh`

```makefile
dev:     # 后端 spring-boot:run(自动重启) + 前端 Vite dev(5174, 代理→8080)
	mvn -pl temporedata-server spring-boot:run &
	(cd temporedata-ui && npm install && npm run dev)
dev-ui:  # 后端 spring-boot:run + 前端真实构建 (file:dist, watch 自动 rebuild)
	mvn -pl temporedata-server spring-boot:run &
	(cd temporedata-ui && npm run build -- --watch)
```

> `npm run build -- --watch` 让前端一改即重出 `dist`，后端从磁盘实时读取，全程无 `mvn package`。

---

## 5. 变更清单与验收

**代码/配置改动**：
1. `temporedata-server/pom.xml`：加 `spring-boot-devtools`（runtime/optional）。
2. `temporedata-ui/vite.config.js`：`outDir` → `./dist`。
3. `WebMvcConfig.java`：静态源改为"外部目录优先 + 回退 `classpath:/static/`"，SPA fallback 不变。
4. 根 `Makefile`（或 `bin/dev.sh`）：`dev` / `dev-ui` 目标。

**验收**：`mvn -pl temporedata-server spring-boot:run` 可起 8080 无需先 `package`；改 `.vue/.js` 前端即时热更或 `dist` watch 重出后刷新即见；改 Java 类 DevTools 自动重启；`mvn -pl temporedata-server -am package` 仍能产出可发布 `temporedata.jar`（生产兼容回退 `classpath:/static` 或挂卷）。

---

## 6. 生产/发布（不变，仍可打包）

- CI：`npm run build`（产出 `dist`）→ 可选复制进 `src/main/resources/static` 随 jar 内嵌，**或**镜像挂载 `dist` 卷并设 `temporedata.ui.static-dir=file:/app/ui`。
- `-Psecurity`（SAST/SCA/SBOM）与 `-Pinclude-engine-*`（引擎内嵌）保持不变，仅发布链路生效。

---

## 7. 风险与边界

- DevTools 自动重启可能打断长查询/定时任务瞬间，仅限开发 profile，生产 jar 不含该依赖。
- `file:` 静态目录需与进程工作目录一致；用 `temporedata.ui.static-dir` 绝对路径规避，容器内改挂载点。
- 前端 5174 与后端 8080 端口不变，避免冲突（沿用 Spring CORS 白名单）。
- 不改变 API 契约、依赖 BOM、数据源插件与安全/脱敏执行链。

> **结论**：通过"后端 `spring-boot:run`+DevTools、前端独立 `dist/` 由后端 `file:` 外部读取、根 Makefile 一键启动"，把 `mvn package` 从**每次运行**降级为**仅发布动作**。
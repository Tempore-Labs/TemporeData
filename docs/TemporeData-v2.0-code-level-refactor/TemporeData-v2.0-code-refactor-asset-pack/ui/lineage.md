# 数据血缘模块 UI 设计（Lineage UI）

> 描述血缘分析页面（路由 `/#/lineage`，新版 `LineageV2.vue`）的界面与交互完整设计：4 区块布局、Toolbar 7 分组、G6 画布、三段式详情面板、3 个写对话框统一 FormSchema、6 条快捷键、i18n / a11y / 主题、路由分叉与真实浏览器走查。
>
> 前后端数据契约与血缘引擎的深层设计见 [../design/module-lineage.md](../design/module-lineage.md)；通用规范见 [design-principles.md](design-principles.md)；运行指南见 [../zh/lineage.md](../zh/lineage.md)。

---

## 1. 页面结构：4 区块布局（新版 V2）

血缘分析以 **4 区块布局** 呈现，替代旧的单画布结构：

| 区块 | 说明 |
|---|---|
| **[B] 资产树** `LgAssetTree` | 左侧资产导航树，宽 260px（折叠 48px）；🌳 资产树（Domain→Owner→Table 三级懒加载）/ 🕒 最近 / 📑 书签 三 Tab；由当前图数据按 domain→owner→name 分组 |
| **[A] Toolbar** | 顶部工具栏，1 行 7 分组，高 56px；图加载控制 + 过滤 + 图层 + 布局 + 动作 |
| **[C] 中央画布** `LineageGraph` | AntV G6 只读图：空态 / 加载骨架 / 图；Minimap、图例 Legend、节点拖拽、边 Popover、右键上下文菜单 |
| **[D] 详情面板** | 右侧三段式：§1 资产元信息（可展开治理 / FQN 复制 / DQ 徽章）· §2 字段列表（Tab + 搜索 + 列高亮）· §3 关联节点（方向 Tab + 删除 + 添加关联）+ 动作条 |
| **状态栏 footer** | 连接状态 · 节点/边 · 渲染耗时 · 权限 chip（新增区块，高 28px） |

```
┌─────────────────────── Layout Shell (54px) ──────────────────────────────┐
├──────┬────────────────────────────────────────────────────────┬─────────┤
│ [B]  │ [A] Toolbar (56px; 7 分组 ①搜索②级别③过滤④Dim/Hide        │ [D]     │
│ 资产 │        ⑤方向深度⑥图层布局⑦动作)                          │ 详情面板 │
│ 树   ├────────────────────────────────────────────────────────┤ (380px)  │
│(260  │ [C] Canvas · Minimap(右下) · Legend(左下) ·             │ §1 资产  │
│ px)  │  Stats(左上) · 全屏/截图锁(右上)                       │ §2 字段  │
│      │                                                        │ §3 关联  │
├──────┴────────────────────────────────────────────────────────┴─────────┤
│  状态栏（连接 · 节点/边 · 渲染ms · 权限 chip · 缩放 87%）(28px)            │
└──────────────────────────────────────────────────────────────────────────┘
```

**断点矩阵**：

| 断点 | 视口 | 资产树 B | 详情 D | 行为 |
|---|---|---|---|---|
| XL | ≥1680 | 280px | 420px | 4 区块并排，7 组全显 |
| L | 1280–1679 | 260px | 380px | 4 区块并排，7 组全显 |
| M | 996–1279 | 240px 图标态 | 360px | ④模式并入③末尾 chip；⑤深度 chip |
| S | 768–995 | 抽屉（Burger） | 右侧 Drawer | ⑦动作下拉化（`… 更多`） |
| XS | <768 | 抽屉 | Drawer | 工具栏垂直堆叠 2 行；Canvas ≥ 60vh |

---

## 2. Toolbar 7 分组

| 组 | 组名 | 控件（从左到右） | 设计意图 |
|---|---|---|---|
| ① | 加载起点 | 根节点搜索（`el-select remote`）+ 重建按钮 + 概览切换 | 弹性 |
| ② | 图级别 | `radio-group`：表级 / 字段级 | 160px |
| ③ | 多维过滤 | `Domain(110) + Owner(100) + Tag(100) + Column(140 filterable)` | 固定 450 |
| ④ | 过滤模式 | `radio-button`：Dim / Hide | 100px |
| ⑤ | 方向·深度·类型 | `Direction(110) + Depth(90) + NodeType(120)` + `Clear All` | 340px |
| ⑥ | 图层·布局·分析 | Chip 组（实体 / 字段 / 质量）+ `Layout(force/layer/radial)` + 环检测 | 400px |
| ⑦ | 动作 | 路径 + 热度 + SQL + 添加血缘（`canWrite` 门禁）+ 导出下拉（PNG/SVG/CSV）+ 连接状态 tag | 弹性右对齐 |

- 组间用浅灰竖分隔条 + 8px padding 视觉隔离；1080p 下各固定组 ≈ 1290px，单行不挤压。
- `Clear All` 在 ⑤ 末尾（触发 `resetFilters`）。
- **Dim / Hide 过滤模式**：Dim 用 opacity=0.15 保留上下文（默认）；Hide 直接裁剪非命中数据（预裁剪到 `viewGraphData`）。

---

## 3. G6 画布（只读）与画布控件

**只读定位**：血缘分析本质"读多写少"，画布内**不直接连线 / 新建节点**；所有"写操作"经对话框 → 调用后端保留的编辑 API（PUT / PATCH / DELETE `/api/lineage/edge/{from}/{to}`）→ 成功后 `store.loadGraph()` 全量重绘。所有写按钮带权限位判断（`hasPerm('LINEAGE:WRITE')`）双门禁。

**图层（一图三模）**：工具栏「图层」Chip 控制渲染模式——
- `ENTITY` 实体层（默认开）：显示 TABLE / TASK / REPORT / DATA_SOURCE 节点 + 主流转边。
- `COLUMN` 字段层：模式 A 嵌入（选中节点卡片内展开列清单 + 边 Popover 显示 fieldLinks）；模式 B 高亮（关闭 ENTITY，画布渲染列级小方块 `[72,24]` + 紫色 `#A78BFA` 列边）。当 `graphData.nodes > 200` 时仅允许模式 A。
- `DATA_QUALITY` 质量层：在节点卡片右上角叠 DQ 徽章（0-100，红 <60 / 黄 60–80 / 绿 ≥80）。

注意 `graphLevel`（表级 / 字段级，决定 `/graph` 请求粒度）与图层 Chip（决定前端渲染层）是**双层语义**。

**布局引擎（L3，替代 ELK）**：不引入 elkjs，用 G6 内置三档——
- `layer`（默认）：`dagre`，`rankdir:'LR', nodesep:32, ranksep:120`；节点 >50 时 `ranksep:160, nodesep:24`。
- `force`：`linkDistance:150`，用于交叉关系复杂场景。
- `radial`：`unitRadius:160`，以根节点为中心的溯 / 影响分析。

**小地图 Minimap（L4）**：G6 自绘 Canvas 小地图（180×120，右下角），支持点击跳转视口，升级项：画布内**拖拽黄色视口矩形**、滚轮缩放、按分类上色（root 黄 / up 绿 / down 蓝 / agg 灰）、浅色背景。

**拖放（L5）**：节点可拖拽微调位置（G6 `drag-element`），写入会话级 `layoutOverrides`（`sessionStorage`），不写回后端，切换布局 / 根节点时清空；详情面板提供「清除位置覆盖」。

**画布控件组（右上）**：全屏按钮 ⛶ + **截图锁定**开关（开启后导出 PNG 时包含图例与小地图）；按钮统一 30×30。

**交互基线条目（不可变更）**：
- 根节点着色 `#FBBF24`、上游 `#10B981`、下游 `#38BDF8`、AGG `#64748B`。
- 字段高亮紫色 `#8B5CF6`、3.5px 宽 + 阴影 10px。
- Minimap 白底 + 分色 + 拖拽视口矩形 + 滚轮缩放。
- 双轨状态更新：状态通道（高亮 / 选中 / dim）纯 `setElementState` 增量；结构通道（根节点 / 图层 / 数据重载）单一 `render()` 入口；禁止在 mousemove / dragevent 回调触发 `render`。

---

## 4. 三段式详情面板

### §1 资产元信息
面包屑 + 表名与 DQ 徽章（颜色 + 文字 + tooltip）+ 类型 / Domain / Owner + Tags（超 3 个收起为 `+N`）；「展开治理信息」默认折叠：FQN / 血缘版本 / 更新时间 / 最后编辑者 / 行级治理策略 / 最近同步 / 下游行级质量。

### §2 字段列表
Tab（全部 / 源 / 目标 / 可空 / 主键）+ 搜索 + 排序；点击字段触发 **列级边高亮联动**（`#8B5CF6` 3.5px 紫色边渲染参与映射的血缘边，其余 opacity 0.2）；字段类型从 `/api/meta/table/{tableId}/columns` 或节点元数据回填，缺失显示 `-`。

### §3 关联节点
Tab（全部 / 上游 ← / 下游 →）+ 邻居搜索；每行卡片含方向 chip + 节点类型 + 垃圾桶（`canWrite` gating）；底端「＋ 添加关联节点」全宽按钮（进入新建血缘对话框，预填当前节点为源）；底部动作条（复制 FQN / 清除位置覆盖 / 新窗口聚焦）。

---

## 5. 三个写对话框 → 统一 FormSchema

三个血缘写对话框（新建 / 编辑 / 资产库加血缘）共享一份 `edgeFormSchema`（字段顺序 / 校验 / 标签一致），维护成本从 3× 降到 1×。实现位于 `composables/useLineageEdgeFormSchema.js`。

| Schema 字段 | 类型 | 校验 | 编辑(PATCH) | 新建(PUT) | 资产库加(PUT) |
|---|---|---|---|---|---|
| `sourceId` | string | 非空 & ≠targetId | ✗(URL 路径) | ✓ | ✓ |
| `targetId` | string | 非空 & ≠sourceId | ✗(URL 路径) | ✓ | ✓ |
| `relationType` | enum INSERT/CTAS/VIEW/TASK/API | 非空 | ✓ | ✓ | ✓ |
| `description` | string | ≥8 字符 | ✓ | ✓ | ✓ |
| `sql` | string | — | ✓ | ✓ | ✓ |
| `taskName` | string | — | ✓ | ✓ | ✓ |
| `priority` | enum P0/P1/P2 | 默认 P1 | ✓ | ✓ | ✓ |
| `customTags` | string[] taggable | ≤8 | ✓ | ✓ | ✓ |

对话框默认只展示 4 项必填（src / tgt / relationType / description），高级字段（taskName / sql / priority / customTags）折叠。底部统一用 `LgDialogActionBar`（取消左对齐 + 保存右对齐）。

**写入口 3 处**（边 Popover 是编辑/删除，不属于新建）：
1. 工具栏「➕ 添加血缘」；
2. 详情 §3「＋ 添加关联节点」（预填 `sourceId=selectedNode.id`）；
3. 右键 ctx-menu「添加上游关联 / 添加下游关联」。

保存流程：`canWrite` 运行时二次校验（双门禁）→ `el-form.validate()` → PUT/PATCH → 成功 `ElMessage.success` + `store.loadGraph()` 重绘；失败保持对话框不关闭并回填已填字段。边已存在 → 409 Conflict → 提示"是否转编辑？"。

---

## 6. 6 条全局快捷键

实现于 `composables/useLineageKeyboard.js`，仅在 `/lineage` 路由生效（`route.name === 'Lineage'` 判断）：

| 快捷键 | 功能 |
|---|---|
| `F2` | 聚焦根节点搜索框 |
| `Ctrl/Cmd + G` | 聚焦画布 + 激活画布键盘（方向键 pan、Ctrl+滚轮 zoom） |
| `Ctrl/Cmd + D` | 打开「新建血缘」对话框（`canWrite=false` 时提示缺少权限） |
| `Ctrl/Cmd + E` | 触发导出下拉 |
| `Ctrl/Cmd + Z` | 清除节点位置覆盖（还原布局） |
| `Ctrl/Cmd + 1/2/3` | 快速切图级别：1=表级，2=字段级，3=表级 + 图层开 DQ |

辅助：Esc 关闭对话框或取消选中节点。

---

## 7. i18n / a11y / 主题

### i18n
- 默认语言 `zh-CN`，字典放 `src/locales/lineage.{zh-CN,en-US}.js`，组件内 `t('lineage.xxx')` 访问；多语言切换由 Layout 统一菜单驱动。
- 已抽取 30 条字典（工具栏 / 层级 / 过滤 / 布局 / 动作 / 状态 / 详情段 / 权限提示等中英对照）。
- 业务数据（Owner / Domain / 资产名）与后端返回的 SQL 错误信息不翻译。

### a11y（WCAG 2.1 AA）
- **语义 Landmarks**：`header[banner]`、资产树 `nav`、主区 `main`（内 `section[aria-label=工具栏]` + `section[aria-label=血缘画布]` + `aside[aria-label=资产详情]`）、`footer[contentinfo=状态栏]`；全局 `aria-live="polite"` 反馈区 + `.sr-only` 样式。
- **对比度**：正文 ≥4.5:1；muted 建议用 `#64748B`（≈5.1:1）；辅助/图标按钮必须有 `aria-label`。
- **aria 补齐**：资产树 Tab `role=tablist/tab/aria-selected`、图层 chip `role=group`、垃圾桶 `aria-label="移除与 {name} 的血缘"`、画布 `role=img` + `aria-label` 替代文本、Minimap / 空态插画 `aria-hidden`。
- **焦点管理**：Tab 顺序贯穿 B→A→D→C→状态栏；画布 Tab 用 `data-graph-focus` 激活；对话框打开 trap focus、关闭后焦点返回触发按钮。

### 主题
- 浅色主题：`--lg-bg-canvas: #F5F7FA`，画布网格线轻量化；`--lg-*` 系列 CSS 变量见 [design-principles.md](design-principles.md) §4。
- 暗色模式保留 `[data-theme="dark"]` 变量接口，不在本次默认落地。
- G6 画布初始化时 `getComputedStyle` 读取 CSS 变量喂养 node/edge 样式；`data-theme` mutation observer 监听主题变化触发重渲染（唯一允许的非数据触发 render 场景）。

---

## 8. 路由分叉 v2 | legacy

- `LineageEntry.vue` 按 `?layout=v2|legacy` 选择：默认 v2（新版 4 区块 `LineageV2.vue`），`legacy` 回退旧 3 区块 `Lineage.vue`；选择记忆于 `localStorage.td_lineage_layout`；新旧视图按需分包。
- **深链参数**（全可选，URL 双向同步 debounce 500ms）：
  `root`(根 id/FQN)、`layout`(v2|legacy)、`dir`(up/down/both)、`depth`(1-5, 默认 2)、`level`(table|column)、`layoutMode`(force/layer/radial)、`layers`(e,c,d 或紧凑 ecd)、`fDomain/fOwner/fTag/fCol`(多维过滤值)、`fMode`(dim|hide)、`focus`(启动选中节点)、`hlCol`(启动高亮列)、`b`(书签 id)、`selNodeType`。
- 示例：`/lineage?root=sales.daily_fact&dir=both&depth=2&level=column&layers=e,c,d&fMode=dim&focus=dws.sales_monthly&hlCol=sale_date`。

---

## 9. 状态管理（Pinia `useLineageStore`）

基于现有 `stores/lineage.js` 扩展，**现有字段保留不删**，新增书签 / 最近浏览 / 历史栈等字段。持久化规则：
- **URL.sync**：由 `useRoute().query` 双向同步（debounce 500ms），不写 localStorage。
- **localStorage**：`bookmarks`(≤100)、`recentNodes`(FIFO≤10)、`treeCollapsedNodes`、`toolbarGroupCollapsed`。
- **sessionStorage**：`layoutOverrides`（会话级，刷新保留、关闭清理）。
- **in-memory**：其余字段，避免大对象序列化。

关键新增 state：`bookmarks`、`recentNodes`、`historyStack`(≤20)、`focusFilters`、`treeCollapsedNodes`、`wsConnected`、`renderStats`、`toolbarGroupCollapsed`。

---

## 10. 迁移路径（Feature Flag 并行）

| 阶段 | 内容 | 合入条件 / 回滚 |
|---|---|---|
| **Phase-A** 结构骨架 | 路由分叉（A1）、4 区块布局 + 状态栏（A2）、Toolbar 7 分组（A3）、资产树（A4）、空态 / 骨架（A5），画布仍复用 `LineageGraph.vue` | 默认 layout=v2；legacy 可回退；功能完整可用 |
| **Phase-B** 深度优化 | 三段式详情（B1）、3 对话框 Schema 归一（B2）、书签流程（B3）、6 快捷键（B4）、a11y/i18n（B5） | 单 PR 可回滚 |
| **Phase-C**（可选） | 删除 `views/Lineage.vue`（legacy 备份到 tag）| 稳定 ≥2 周 |

**回滚 3 级**：L-1 单用户 `?layout=legacy`（≤1min）；L-2 全局改回 legacy（≤15min）；L-3 git revert 单 PR（≤10min）。

---

## 11. 真实浏览器走查（回归验证）

`scripts/lineage-walkthrough.cjs` 借助 Chrome 无头 + CDP（无 Playwright/Puppeteer 依赖，仅 Node 内置 WebSocket）自动验证：血缘页 V2 挂载、admin 写门控、根节点图渲染、搜索下拉隐藏 stats 覆盖层、无 console 报错。

```bash
node scripts/lineage-walkthrough.cjs --root tbl:dws_sales_daily --open-search --out /tmp
# --root        指定根节点以渲染图（省略则只验挂载与写门控）
# --open-search 额外校验"打开搜索下拉时隐藏画布 stats 覆盖层"
# --frontend/--backend 可覆盖地址；输出 WALK_OK/FAIL，exit code 0/1
# 前置：后端(8080) 与前端(5174) 需在运行；本机需 Chrome
```

手工走查 TC（QA checklist）：TC-1 空态引导（3 入口）、TC-2 资产树浏览、TC-3 多维过滤 × 列高亮联动、TC-4 新建血缘（有 / 无写权限差异）、TC-5 分析联动（环 / SQL / 路径）、TC-6 导出（PNG/SVG/CSV，PNG 两阶段下载）、TC-7 断点折叠。

**关键约束（不可违反）**：不新增 `elkjs / react-flow / html2canvas / dagre-d3 / @vue-flow/core` 等第三方依赖；性能基线：<50 节点首渲 ≤400ms、50–200 节点 ≤1.2s、字段高亮切换 ≤100ms。
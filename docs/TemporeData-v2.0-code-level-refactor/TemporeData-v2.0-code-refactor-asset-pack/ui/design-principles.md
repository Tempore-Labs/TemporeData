# 通用 UI 设计原则与规范要点（Design Principles）

> 用于指导平台前端界面的统一性与可用性。章节覆盖：设计目标、四条核心交互原则（Fitts / 渐进披露 / 双轨反馈 / 无障碍先行）、设计 Token（色板 / 间距 / 圆角 / 主题变量）、Element Plus 主题覆盖、以及组件规范的表头约定。

---

## 1. 设计目标

平台前端（Vue 3 + Element Plus + Pinia）追求三件事：

1. **效率（Explore Efficiency）**：把高频操作（进入 → 查看 / 治理）的平均步骤数压到最低，提供多入口、书签、深链直达等快捷通道。
2. **可及（Edit Reachability）**：对具备写权限的用户，关键"创建 / 编辑 / 删除"动作在任意选中态下都有 ≥2 处可达入口。
3. **一致与可扩展（Consistency & Scalability）**：通过可复用组件（Props / Emits / Slots 合同）、统一表单 Schema、单一权限键、四级断点保证界面一致与后续可扩展。

---

## 2. 四条核心交互原则

### 2.1 Fitts 定律 / 目标可达性

- 重要且高频的控件（根节点搜索、添加、导出）置于易达区域（工具栏、页脚动作条），而非深藏菜单。
- 常用操作按钮尺寸 ≥ 28px（`--el-component-size-small`），间距按 4px 基数（`$space-unit`）取整。
- 任意选中态下写入操作均可在 500px 范围内被触达（工具栏 / 上下文面板 / 点选 / 右键）。

### 2.2 渐进披露（Progressive Disclosure）

- 默认只暴露高频信息，低频 / 高级信息折叠，"展开更多"后再显示。例：详情面板默认 6 行摘要、工具栏仅放 7 组高频控件、对话框默认 4 项必填 + 高级字段折叠。
- 复杂度被分层承载，首屏不会因信息过载而失衡。

### 2.3 双轨反馈（Feedback Dual-Track）

- 操作反馈同时在**视觉通道**（Toast + 状态样式）与**语义通道**（`aria-live` 区域）呈现。
- 例：删除成功 → `ElMessage.success` + 边 `filter-dim`（opacity 0.15），随后 300ms 移除 + `aria-live="polite"` 播报。

### 2.4 无障碍先行（A11y by Default）

- 所有交互位都有键盘可达路径（Tab / Enter / Esc / Arrow）。
- 颜色不单独作为信息载体（如 DQ 分数同时用颜色 + 文字 + Tooltip）。
- 文本对比度 ≥ 4.5:1（大型文本 ≥ 3:1），见 §4 色板。

---

## 3. 设计 Token 体系

设计 Token 分为三类来源：

- **E** = 从现有实现提取（硬编码值收敛而来）
- **EP** = Element Plus 默认（未改动）
- **N** = 新增建议（带使用边界说明）

### 3.1 布局尺寸 & 间距

| Token | 值 | 来源 | 使用边界 |
|---|---|---|---|
| `$layout-gap` | **12px** | E | 页面区块间间距；≤ 768 断点降为 8px |
| `$header-h` | **54px** | E | 顶栏高度 |
| `$tb-h` | **56px** | N（建议） | 工具栏高度；M 断点降为 48px |
| `$sb-h` | **28px** | N | 底部状态栏高度 |
| `$space-unit` | **4px** | EP | 所有内边距 / 外边距必须用 4n（4/8/12/16/20/24） |

### 3.2 圆角 & 阴影

| Token | 值 | 来源 | 使用边界 |
|---|---|---|---|
| `$r-sm / $r-md / $r-lg` | 4 / 6 / 10 px | E | 按钮 sm、卡片 md、对话框 lg |
| `$shadow-card` | `0 1px 2px rgba(15,23,42,.06), 0 1px 3px rgba(15,23,42,.08)` | N | 面板 / 对话框 / 空态卡片；禁用大阴影 |
| `$shadow-overlay` | `0 8px 24px rgba(15,23,42,.12)` | N | 右键菜单 / 小卡片 popover |

### 3.3 色板（浅色主题）

| Token | 值 | 来源 | 使用边界 |
|---|---|---|---|
| `$bg-canvas` | `#F5F7FA` | E | 画布底 + 网格线 `rgba(148,163,184,.14)` |
| `$surface` | `#FFFFFF` | E | 面板 / 对话框 / 工具栏底 |
| `$text-primary / secondary / muted` | `#0F172A / #334155 / #94A3B8` | E | 保证 4.5:1（primary）与 4.2:1（secondary）对比度；muted 建议改用 `#64748B` |
| `$accent-root / up / down / agg` | `#FBBF24 / #10B981 / #38BDF8 / #64748B` | E | 节点 chip + 邻居卡方向指示 |
| `$accent-highlight` | `#8B5CF6` | E | 字段级边高亮 + Tab 激活 + 重点强调 |
| `$dq-A / B / C / D` | `#059669 / #0891B2 / #D97706 / #DC2626` | N | DQ 徽章色；颜色不单独作载体（带文字 A+/B） |

### 3.4 字号 & 行高

| Token | 值 | 来源 |
|---|---|---|
| `$fs-xs / sm / md / lg / xl` | 11 / 12 / 14 / 16 / 20 px | EP |
| `$lh` | `1.55 × fs`；标题 `1.3 × fs` | EP |

---

## 4. 主题（CSS Variable 根变量集）

```css
:root, [data-theme="light"] {
  --lg-layout-gap: 12px;  --lg-header-h: 54px;
  --lg-tb-h: 56px;        --lg-sb-h: 28px;  --lg-space: 4px;
  --lg-r-sm: 4px; --lg-r-md: 6px; --lg-r-lg: 10px;
  --lg-shadow-card: 0 1px 2px rgba(15,23,42,.06), 0 1px 3px rgba(15,23,42,.08);
  --lg-shadow-overlay: 0 8px 24px rgba(15,23,42,.12);
  --lg-bg-canvas: #F5F7FA; --lg-bg-surface: #FFFFFF;
  --lg-text: #0F172A; --lg-text-sub: #475569; --lg-text-muted: #64748B;
  --lg-accent-root: #FBBF24; --lg-accent-up: #10B981;
  --lg-accent-down: #38BDF8; --lg-accent-agg: #64748B; --lg-accent-hl: #8B5CF6;
  --lg-dq-A: #059669; --lg-dq-B: #0891B2; --lg-dq-C: #D97706; --lg-dq-D: #DC2626;
  --lg-fs-xs: 11px; --lg-fs-sm: 12px; --lg-fs-md: 14px; --lg-fs-lg: 16px; --lg-fs-xl: 20px;
  --lg-lh: 1.55;
}
```

> 暗色模式保留接口（`[data-theme="dark"]` 覆盖背景 / 文字 / 阴影变量），不在本次默认落地；尚未落地时可通过 `getComputedStyle` 读取 CSS 变量喂养画布渲染库。

---

## 5. Element Plus 主题覆盖清单

建议追加到 `styles/element-overrides.scss`，**仅覆盖 5 类关键变量**，不破坏 Element Plus 默认 key：

```scss
--el-color-primary:        #2563EB;   // N 建议 primary（与画布根节点黄不冲突）
--el-color-success:        #10B981;   // E = upstream 色
--el-color-warning:        #FBBF24;   // E = root 色
--el-color-danger:         #DC2626;   // E = D-level DQ 色
--el-border-radius-base:   6px;       // N 现状 4px
--el-font-size-base:       14px;      // EP 保持
--el-component-size-small: 28px;      // EP 小按钮高度，匹配工具栏
--el-component-size:       32px;      // EP 默认，匹配工具栏控件
```

---

## 6. 组件规范表头约定

可复用组件统一采用 `Lg` 前缀（Lineage 专属，避免与全局组件冲突），放 `components/lineage/` 目录。规范表格约定如下：

- **事件命名**：对外事件统一 **kebab-case**（如 `add-edge`）；`v-model` 语法糖单独列 `update:modelValue` / `update:<propName>`。
- **Props 表**：列 `属性 | Props | 类型 | 默认 | 说明`。
- **Emits 表**：列 `对外事件 | Emits | Payload | 说明`。
- **Slots 表**：列 `Slot 名 | 说明`。

### 已有组件清单（血缘模块）

| 组件 | 用途 |
|---|---|
| `LgAssetSearch` | 资产远程搜索器（根节点 / 对话框 src·tgt / 路径起终点） |
| `LgFilterBarGroup` | 多维过滤 + 模式 + 方向 + 深度 + 类型封装 |
| `LgLayerChipGroup` | 图层 3 芯片（实体 / 字段 / 质量） |
| `LgDqBadge` | 数据质量徽章 + tooltip |
| `LgCopyFqn` | FQN 复制按钮 |
| `LgNeighborCard` | 关联节点卡片（方向 chip + 名称 + 垃圾桶） |
| `LgNodeTypeChip` | 节点类型 chip |
| `LgAssetTree` | 左侧资产导航树（🌳 资产 / 🕒 最近 / 📑 书签 三 Tab） |
| `LgDialogActionBar` | 对话框标准底栏（取消 / 保存） |
| `LgEmptyState` | 空态卡片（探索 / 数据 / 错误） |
| `LgLoadingSkeleton` | 加载骨架（6 椭圆 + 10 线） |
| `LgColumnList` | 字段列表（Tab + 搜索 + 高亮联动） |
| `LgStatusTab` | 邻居 / 字段通用 Tab switcher |
| `LgGraphStats` | 画布左上角状态面板（节点 / 边 / 渲染耗时） |

> 每个组件的完整 Props / Emits / Slots 明细见 [lineage.md](lineage.md) 表哥，及源码 `components/lineage/`。
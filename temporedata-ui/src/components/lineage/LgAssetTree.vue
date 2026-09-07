<template>
  <div class="lg-tree" role="navigation" aria-label="Lineage asset tree">
    <div class="lg-tree-tabs" role="tablist">
      <button
        v-for="t in tabs"
        :key="t.key"
        class="lg-tree-tab"
        role="tab"
        :class="{ active: activeTab === t.key }"
        :aria-selected="activeTab === t.key"
        @click="activeTab = t.key"
      >{{ t.icon }} {{ t.label }}</button>
    </div>

    <!-- 🟢 Asset tree tab -->
    <template v-if="activeTab === 'tree'">
      <el-input v-model="keyword" size="small" placeholder="搜索资产（域/负责人/表）" clearable class="lg-tree-search" />
      <div class="lg-tree-scroll">
        <div v-if="!filteredTree.length" class="lg-tree-empty">从图数据中暂无可分组的资产</div>
        <div v-for="domain in filteredTree" :key="domain.key" class="tr-domain-wrap">
          <div
            class="tr-row tr-domain"
            role="button"
            :aria-expanded="!isCollapsed(domain.key)"
            @click="toggle(domain.key)"
          >
            <span class="tr-arrow">{{ isCollapsed(domain.key) ? '▶' : '▼' }}</span>
            <span class="tr-name">{{ domain.name }}</span>
            <span class="tr-count">{{ domain.children.length }}</span>
          </div>
          <template v-if="!isCollapsed(domain.key)">
            <div v-for="owner in domain.children" :key="owner.key" class="tr-owner-wrap">
              <div
                class="tr-row tr-owner"
                role="button"
                :aria-expanded="!isCollapsed(owner.key)"
                @click="toggle(owner.key)"
              >
                <span class="tr-arrow">{{ isCollapsed(owner.key) ? '▶' : '▼' }}</span>
                <span class="tr-name">{{ owner.name === '未知' ? '(未知负责人)' : owner.name }}</span>
                <span class="tr-count">{{ owner.children.length }}</span>
              </div>
              <template v-if="!isCollapsed(owner.key)">
                <div
                  v-for="leaf in owner.children"
                  :key="leaf.key"
                  class="tr-leaf"
                  :class="{ active: leaf.id === activeId }"
                  role="button"
                  :aria-disabled="false"
                  @click="onSelect(leaf)"
                >
                  <span class="tr-leaf-name" :title="leaf.name">{{ leaf.name }}</span>
                  <el-tag v-if="leaf.nodeType" size="small" class="tr-leaf-type" :type="typeTag(leaf.nodeType)">{{ leaf.nodeType }}</el-tag>
                </div>
              </template>
            </div>
          </template>
        </div>
      </div>
    </template>

    <!-- 🕒 Recent tab -->
    <template v-else-if="activeTab === 'recent'">
      <div class="lg-tree-scroll">
        <div v-if="!recent.length" class="lg-tree-empty">暂无最近浏览</div>
        <div v-for="r in recent" :key="r.id" class="tr-leaf" role="button" @click="emit('recent-click', r)">
          <span class="tr-leaf-name" :title="r.name">{{ r.name }}</span>
          <el-tag v-if="r.nodeType" size="small" class="tr-leaf-type" :type="typeTag(r.nodeType)">{{ r.nodeType }}</el-tag>
        </div>
      </div>
    </template>

    <!-- 📑 Bookmarks tab -->
    <template v-else>
      <div class="lg-tree-head-row">
        <span class="lg-tree-sub">我的书签（{{ bookmarks.length }}）</span>
        <el-button link size="small" :icon="Plus" :disabled="!rootNodeId" :aria-disabled="!rootNodeId" title="收藏当前根节点" @click="emit('add-bookmark')">收藏根节点</el-button>
      </div>
      <div class="lg-tree-scroll">
        <div v-if="!bookmarks.length" class="lg-tree-empty">暂无书签</div>
        <div v-for="b in bookmarks" :key="b.id" class="tr-leaf tr-bookmark" role="button" @click="emit('bookmark-click', b)">
          <span class="tr-leaf-name" :title="b.name">{{ b.name }}</span>
          <el-button link type="danger" size="small" :icon="Close" title="移除书签" @click.stop="emit('bookmark-remove', b.id)" />
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
/**
 * [B] Asset tree: three tabs — 🌳 assets (grouped from the current graphData by
 * domain → owner → name, NOT fetched from backend), 🕒 recent (localStorage),
 * and 📑 bookmarks (localStorage). Leaf selection emits `select`; bookmarks and
 * recent restore via their dedicated events. Collapse state is persisted through
 * the parent so it can be stored on the lineage store.
 */
import { ref, computed } from 'vue'
import { Plus, Close } from '@element-plus/icons-vue'

const props = defineProps({
  tree: { type: Array, default: () => [] },
  recent: { type: Array, default: () => [] },
  bookmarks: { type: Array, default: () => [] },
  activeId: { type: String, default: '' },
  collapsed: { type: Object, default: () => ({}) },
  rootNodeId: { type: String, default: '' },
  canAddBookmark: { type: Boolean, default: true }
})

const emit = defineEmits([
  'select',
  'recent-click',
  'bookmark-click',
  'bookmark-remove',
  'add-bookmark',
  'toggle-collapse'
])

const tabs = [
  { key: 'tree', icon: '🌳', label: '资产树' },
  { key: 'recent', icon: '🕒', label: '最近' },
  { key: 'bookmark', icon: '📑', label: '书签' }
]
const activeTab = ref('tree')
const keyword = ref('')

const filteredTree = computed(() => {
  const kw = (keyword.value || '').trim().toLowerCase()
  if (!kw) return props.tree
  return props.tree
    .map(d => ({
      ...d,
      children: d.children
        .map(o => ({
          ...o,
          children: o.children.filter(l => l.name.toLowerCase().includes(kw))
        }))
        .filter(o => o.name.toLowerCase().includes(kw) || o.children.length)
    }))
    .filter(d => d.name.toLowerCase().includes(kw) || d.children.length)
})

function isCollapsed(key) {
  return props.collapsed?.[key] === true
}

function toggle(key) {
  emit('toggle-collapse', key)
}

function onSelect(leaf) {
  emit('select', leaf)
}

function typeTag(t) {
  return t === 'TABLE' ? 'primary' : t === 'COLUMN' ? 'success' : t === 'TASK' ? 'warning' : 'info'
}
</script>

<style scoped>
.lg-tree {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--lg-surface, #FFFFFF);
  border-right: 1px solid #E8EAF0;
  font-size: 12px;
  overflow: hidden;
}
.lg-tree-tabs {
  display: flex;
  border-bottom: 1px solid #E8EAF0;
}
.lg-tree-tab {
  flex: 1;
  border: none;
  background: transparent;
  padding: 9px 4px;
  font-size: 12px;
  color: #64748B;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  white-space: nowrap;
}
.lg-tree-tab.active {
  color: #2563EB;
  border-bottom-color: #2563EB;
  font-weight: 600;
}
.lg-tree-search {
  padding: 8px;
}
.lg-tree-head-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px 4px;
}
.lg-tree-sub {
  color: #64748B;
  font-size: 11px;
  font-weight: 600;
}
.lg-tree-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 4px 6px 12px;
}
.lg-tree-empty {
  color: #94A3B8;
  font-size: 12px;
  padding: 12px 8px;
  text-align: center;
}
.tr-row {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 6px;
  border-radius: 4px;
  cursor: pointer;
  color: #334155;
}
.tr-row:hover {
  background: #F1F5F9;
}
.tr-domain {
  font-weight: 700;
  color: #0F172A;
  margin-top: 2px;
}
.tr-owner {
  font-weight: 600;
  color: #475569;
  padding-left: 16px;
}
.tr-arrow {
  width: 12px;
  text-align: center;
  color: #94A3B8;
  font-size: 10px;
}
.tr-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tr-count {
  color: #CBD5E1;
  font-size: 11px;
}
.tr-leaf {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 6px;
  padding-left: 34px;
  border-radius: 4px;
  cursor: pointer;
  color: #334155;
}
.tr-leaf:hover {
  background: #F1F5F9;
}
.tr-leaf.active {
  background: #E0EAFE;
  color: #1D4ED8;
}
.tr-leaf-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: Menlo, Consolas, monospace;
  font-size: 11px;
}
.tr-leaf-type {
  flex-shrink: 0;
  transform: scale(0.85);
}
.tr-bookmark {
  padding-left: 12px;
}
</style>
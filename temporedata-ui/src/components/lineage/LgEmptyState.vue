<template>
  <div class="lg-empty" role="region" aria-label="Lineage empty state">
    <div class="lg-empty-hero">
      <div class="lg-empty-badge">🔎</div>
      <h2 class="lg-empty-title">选择一个数据资产，开启你的血缘探索之旅</h2>
    </div>

    <div class="lg-empty-cards">
      <!-- Hot assets -->
      <div class="lg-card">
        <div class="lg-card-head"><span>🔥 热门资产</span><el-button v-if="!hotFailed" link size="small" @click="reloadHeat">刷新</el-button></div>
        <ul v-if="hotItems.length" class="lg-card-list">
          <li v-for="h in hotItems" :key="h.id" class="lg-card-item" @click="emit('select', { id: h.id, name: h.name, nodeType: h.nodeType })">
            <span class="lg-card-name" :title="h.name">{{ h.name }}</span>
            <span class="lg-card-deg">↑{{ h.degree }}</span>
          </li>
        </ul>
        <div v-else class="lg-card-empty">{{ hotFailed ? '热度数据不可用' : '暂无热度数据' }}</div>
      </div>

      <!-- Recent -->
      <div class="lg-card">
        <div class="lg-card-head"><span>🕒 最近浏览</span></div>
        <ul v-if="recent.length" class="lg-card-list">
          <li v-for="r in recent" :key="r.id" class="lg-card-item" @click="emit('recent-click', r)">
            <span class="lg-card-name" :title="r.name">{{ r.name }}</span>
          </li>
        </ul>
        <div v-else class="lg-card-empty">暂无最近浏览</div>
      </div>

      <!-- Bookmarks -->
      <div class="lg-card">
        <div class="lg-card-head"><span>📑 我的书签</span></div>
        <ul v-if="bookmarks.length" class="lg-card-list">
          <li v-for="b in bookmarks" :key="b.id" class="lg-card-item" @click="emit('bookmark-click', b)">
            <span class="lg-card-name" :title="b.name">{{ b.name }}</span>
          </li>
        </ul>
        <div v-else class="lg-card-empty">暂无书签</div>
      </div>
    </div>

    <div class="lg-empty-actions">
      <el-button type="primary" :icon="Search" @click="useSearch">使用根节点搜索</el-button>
      <el-input v-model="fqn" class="lg-fqn" placeholder="粘贴 FQN 直达（如 sales.prod.daily_fact）" @keyup.enter="onSubmitFqn">
        <template #append><el-button :icon="Position" @click="onSubmitFqn" :loading="loadingFqn" /></template>
      </el-input>
    </div>
  </div>
</template>

<script setup>
/**
 * Post-empty start guide shown when no root node has been selected.
 * Offers three quick entries (hot / recent / bookmarks) plus a root-node
 * search focus trigger and a direct FQN jump input. Hot data comes from
 * lineageApi.getHeat(); on failure the hot card degrades silently.
 */
import { ref, onMounted } from 'vue'
import { Search, Position } from '@element-plus/icons-vue'
import { lineageApi } from '@/api/modules/lineage'

defineProps({
  recent: { type: Array, default: () => [] },
  bookmarks: { type: Array, default: () => [] }
})

const emit = defineEmits(['select', 'recent-click', 'bookmark-click', 'use-search', 'paste-fqn'])

const hotItems = ref([])
const hotFailed = ref(false)
const loadingFqn = ref(false)
const fqn = ref('')

const FQN_REGEX = /^[\w-]+(\.[\w-]+)+$/

onMounted(() => {
  loadHeat()
})

async function loadHeat() {
  try {
    const res = await lineageApi.getHeat()
    hotItems.value = Array.isArray(res) ? res.slice(0, 5) : []
    hotFailed.value = false
  } catch (e) {
    hotFailed.value = true
    hotItems.value = []
  }
}

function reloadHeat() {
  loadHeat()
}

async function onSubmitFqn() {
  const text = (fqn.value || '').trim()
  if (!text) return
  if (!FQN_REGEX.test(text)) {
    emit('paste-fqn', { fqn: text, valid: false })
    return
  }
  loadingFqn.value = true
  emit('paste-fqn', { fqn: text, valid: true })
  loadingFqn.value = false
}

// When the primary button is clicked, emit so the parent focuses its root search.
function useSearch() {
  emit('use-search')
}
</script>

<style scoped>
.lg-empty {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 24px;
  padding: 24px;
  background: var(--lg-bg-canvas, #F5F7FA);
}
.lg-empty-hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}
.lg-empty-badge {
  font-size: 40px;
  line-height: 1;
}
.lg-empty-title {
  font-size: var(--lg-fs-xl, 20px);
  color: var(--lg-text, #0F172A);
  font-weight: 600;
  margin: 0;
  text-align: center;
}
.lg-empty-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
  width: 100%;
  max-width: 860px;
}
.lg-card {
  background: var(--lg-surface, #FFFFFF);
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.06);
  padding: 12px;
}
.lg-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 8px;
}
.lg-card-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.lg-card-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 6px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}
.lg-card-item:hover {
  background: #EEF2F7;
}
.lg-card-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #374151;
  font-family: Menlo, Consolas, monospace;
}
.lg-card-deg {
  color: #EF4444;
  font-size: 11px;
}
.lg-card-empty {
  color: #94A3B8;
  font-size: 12px;
  padding: 4px 0;
}
.lg-empty-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  width: 100%;
  max-width: 620px;
}
.lg-fqn {
  flex: 1;
}
</style>
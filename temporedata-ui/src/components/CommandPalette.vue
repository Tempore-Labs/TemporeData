<template>
  <!-- Global Command Palette (Ctrl+K). High-risk actions must NOT be exec here; only open Preview. -->
  <teleport to="body">
    <div v-if="open" class="cp-overlay" @click.self="close">
      <div class="cp-panel" role="dialog" aria-label="Command Palette">
        <el-input
          v-model="q"
          ref="inputRef"
          placeholder="搜索资源 / 页面 / Job / Cluster / Incident…"
          :prefix-icon="Search"
          clearable
          autofocus
          @keydown.esc="close"
          @keydown.enter="runFirst"
        />
        <ul class="cp-list">
          <li
            v-for="(item, i) in filtered"
            :key="item.key"
            class="cp-item"
            :class="{ active: i === activeIdx }"
            @mouseenter="activeIdx = i"
            @click="go(item)"
          >
            <span class="cp-label">{{ item.label }}</span>
            <span class="cp-hint">{{ item.kind }}</span>
          </li>
          <li v-if="!filtered.length" class="cp-empty">无匹配项</li>
        </ul>
      </div>
    </div>
  </teleport>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { NAV } from '@/layouts/navigation'
import { canExecute } from '@/permissions'

const router = useRouter()
const open = ref(false)
const q = ref('')
const activeIdx = ref(0)
const inputRef = ref(null)

// Sources: navigation pages + (later) resource/job/cluster search. 
const items = computed(() =>
  Object.values(NAV).map((n) => ({ key: n.path, label: n.label, kind: '页面', path: n.path, risk: 0 }))
)

const filtered = computed(() => {
  const kw = q.value.trim().toLowerCase()
  const base = kw ? items.value.filter((i) => i.label.toLowerCase().includes(kw) || i.path.toLowerCase().includes(kw)) : items.value
  return base.slice(0, 20)
})

function toggle() { open.value = !open.value; if (open.value) nextTick(() => inputRef.value?.focus()) }
function close() { open.value = false; q.value = '' }
function runFirst() { if (filtered.value[activeIdx.value]) go(filtered.value[activeIdx.value]) }
function go(item) {
  if (item.risk && !canExecute(item.perm)) { /* → Action Preview placeholder */ return }
  close()
  router.push(item.path).catch(() => {})
}

watch(() => filtered.value.length, () => { activeIdx.value = 0 })

function onKey(e) { if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') { e.preventDefault(); toggle() } }

defineExpose({ open: open.value ? true : false })
if (typeof window !== 'undefined') window.addEventListener('keydown', onKey)
</script>

<style scoped>
.cp-overlay { position: fixed; inset: 0; z-index: 3000; background: rgba(15, 23, 42, 0.35); display: flex; justify-content: center; align-items: flex-start; padding-top: 14vh; }
.cp-panel { width: 520px; max-width: 92vw; background: #fff; border-radius: 10px; box-shadow: 0 8px 24px rgba(15,23,42,.12); padding: 10px; }
.cp-list { list-style: none; margin: 8px 0 0; padding: 0; max-height: 300px; overflow: auto; }
.cp-item { display: flex; justify-content: space-between; padding: 8px 10px; border-radius: 6px; cursor: pointer; }
.cp-item.active { background: #eff6ff; }
.cp-hint { color: #94a3b8; font-size: 12px; }
.cp-empty { padding: 10px; color: #94a3b8; text-align: center; }
</style>
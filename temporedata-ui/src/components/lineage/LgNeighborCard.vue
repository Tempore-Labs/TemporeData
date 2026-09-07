<template>
  <div class="lg-nb-card" :class="dir" role="button" tabindex="0" @click="emit('focus', nodeId)" @keyup.enter="emit('focus', nodeId)">
    <span class="lg-nb-dir" :aria-label="dir === 'up' ? 'upstream' : 'downstream'">
      {{ dir === 'up' ? '←' : '→' }}
    </span>
    <span class="lg-nb-name" :title="name">{{ name }}</span>
    <el-tag v-if="nodeType" size="small" class="lg-nb-type">{{ nodeType }}</el-tag>
    <button
      type="button"
      class="lg-nb-remove"
      :aria-label="`移除与 ${name} 的血缘`"
      :title="canRemove ? '移除该血缘关系' : '缺少 LINEAGE:WRITE 权限'"
      :disabled="!canRemove"
      :aria-disabled="!canRemove"
      @click.stop="emit('remove', { nodeId, dir })"
    >
      <el-icon><Delete /></el-icon>
    </button>
  </div>
</template>

<script setup>
/**
 * Neighbor card (C-06): a single row in the detail §3 list. Shows a small
 * direction chip (up=green ← , down=sky →), the neighbor name, its node type and
 * an optional remove button gated by canRemove (= canWrite).
 */
import { Delete } from '@element-plus/icons-vue'

defineProps({
  nodeId: { type: String, required: true },
  name: { type: String, required: true },
  nodeType: { type: String, default: '' },
  dir: { type: String, default: 'down' },
  canRemove: { type: Boolean, default: false }
})

const emit = defineEmits(['focus', 'remove'])
</script>

<style scoped>
.lg-nb-card {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  padding: 0 10px;
  border: 1px solid #e8eaf0;
  border-left: 3px solid #38bdf8;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
}
.lg-nb-card.up {
  border-left-color: #10b981;
}
.lg-nb-card:hover {
  box-shadow: 0 2px 8px rgba(74, 144, 226, 0.18);
  border-color: #4a90e2;
}
.lg-nb-dir {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  border-radius: 4px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 800;
  color: #fff;
}
.lg-nb-card.up .lg-nb-dir {
  background: #10B981;
}
.lg-nb-card.down .lg-nb-dir {
  background: #38BDF8;
}
.lg-nb-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--lg-text-sub, #374151);
  font-family: Menlo, Consolas, monospace;
  font-size: 11px;
}
.lg-nb-type {
  flex-shrink: 0;
  transform: scale(0.88);
}
.lg-nb-remove {
  flex-shrink: 0;
  border: none;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  padding: 3px;
  border-radius: 4px;
}
.lg-nb-remove:hover:not(:disabled) {
  color: #dc2626;
  background: #fef2f2;
}
.lg-nb-remove:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
</style>
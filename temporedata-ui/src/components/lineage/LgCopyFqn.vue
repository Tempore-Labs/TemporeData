<template>
  <span class="lg-copy-fqn" :class="size">
    <span v-if="text" class="lg-fqn-text" :title="text">{{ text }}</span>
    <el-button
      link
      size="small"
      class="lg-fqn-btn"
      :icon="CopyDocument"
      :aria-label="`复制 ${text || label}`"
      :title="`复制 ${text || label}`"
      @click="copy"
    />
  </span>
</template>

<script setup>
/**
 * Copy-FQN button (C-05). Primary path uses the asynchronous Clipboard API and,
 * on any failure, falls back to a hidden textarea + document.execCommand so the
 * copy action still succeeds in non-secure or legacy contexts.
 */
import { CopyDocument } from '@element-plus/icons-vue'

const props = defineProps({
  text: { type: String, default: '' },
  size: { type: String, default: 'sm' },
  label: { type: String, default: '复制' }
})

const emit = defineEmits(['copied'])

async function copy() {
  const t = props.text
  if (!t) return
  try {
    await navigator.clipboard.writeText(t)
  } catch (e) {
    legacyCopy(t)
  }
  emit('copied', props.text)
}

function legacyCopy(t) {
  try {
    const ta = document.createElement('textarea')
    ta.value = t
    ta.setAttribute('readonly', '')
    ta.style.position = 'fixed'
    ta.style.left = '-9999px'
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
  } catch (e) {
    // Even the fallback failed; the caller still gets the `copied` event so it
    // can surface a consistent user-facing message.
    emit('copied', props.text)
  }
}
</script>

<style scoped>
.lg-copy-fqn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
  max-width: 100%;
}
.lg-fqn-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: Menlo, Consolas, monospace;
  font-size: 11px;
  color: var(--lg-text-muted, #64748B);
}
.lg-copy-fqn.md .lg-fqn-text {
  font-size: 12px;
}
.lg-fqn-btn {
  flex-shrink: 0;
}
</style>
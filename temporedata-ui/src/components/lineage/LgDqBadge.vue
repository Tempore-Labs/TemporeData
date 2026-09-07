<template>
  <button
    type="button"
    class="lg-dq-badge"
    :class="[levelClass, { compact }]"
    :style="{ background: meta.color }"
    :title="`Data quality ${meta.label}${score != null ? ' (' + score + ')' : ''}`"
    :aria-label="`Data quality level ${meta.label}`"
    @click="onClick"
  >
    <span class="lg-dq-label">{{ meta.label }}</span>
    <span v-if="!compact && score != null" class="lg-dq-score">{{ score }}</span>
  </button>
</template>

<script setup>
/**
 * Data-quality badge (C-04). Maps a 0-100 score to an A+/A/B+/B/C+/C/D level and
 * renders it with the matching DQ accent color. Text level is always shown so the
 * color is never the single carrier of meaning (colour-safe per 06 §6.1.2).
 */
import { computed } from 'vue'

const props = defineProps({
  score: { type: Number, default: null },
  compact: { type: Boolean, default: false }
})

const emit = defineEmits(['click-detail'])

function mapLevel(score) {
  if (score == null) return { label: '–', grade: 'D', color: '#94A3B8' }
  if (score >= 95) return { label: 'A+', grade: 'A', color: '#059669' }
  if (score >= 90) return { label: 'A', grade: 'A', color: '#059669' }
  if (score >= 85) return { label: 'B+', grade: 'B', color: '#0891B2' }
  if (score >= 80) return { label: 'B', grade: 'B', color: '#0891B2' }
  if (score >= 70) return { label: 'C+', grade: 'C', color: '#D97706' }
  if (score >= 60) return { label: 'C', grade: 'C', color: '#D97706' }
  return { label: 'D', grade: 'D', color: '#DC2626' }
}

const meta = computed(() => mapLevel(props.score))
const levelClass = computed(() => 'lg-dq-' + meta.value.grade.toLowerCase())

function onClick() {
  emit('click-detail', props.score)
}
</script>

<style scoped>
.lg-dq-badge {
  border: none;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 1px 7px;
  border-radius: 10px;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  line-height: 1.6;
  cursor: pointer;
  font-family: Menlo, Consolas, monospace;
}
.lg-dq-badge.compact {
  padding: 0 6px;
}
.lg-dq-score {
  opacity: 0.9;
  font-weight: 600;
}
</style>
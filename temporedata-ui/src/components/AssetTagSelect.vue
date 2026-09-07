<template>
  <el-select
    :model-value="modelValue"
    multiple
    collapse-tags
    filterable
    clearable
    :placeholder="placeholder"
    :disabled="disabled || tags.length === 0"
    style="width: 100%"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <template v-for="c in groupedTags" :key="c.classificationId">
      <el-option-group v-if="c.tags.length" :label="c.classificationName || '未分类'">
        <el-option v-for="t in c.tags" :key="t.id" :label="t.name" :value="t.id">
          <span class="tag-option">
            <span class="tag-dot" :style="{ background: t.color || '#409EFF' }"></span>
            {{ t.name }}
            <el-tag v-if="t.status === 'DRAFT'" size="small" type="warning" style="margin-left: 6px">草稿</el-tag>
          </span>
        </el-option>
      </el-option-group>
    </template>
    <template #empty>
      <div class="tag-select-empty">暂无可用标签，请先在「数据标签」中创建</div>
    </template>
  </el-select>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { tagApi } from '@/api/modules/tag'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  placeholder: { type: String, default: '选择标签' },
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue'])

const tags = ref([])

const groupedTags = computed(() => {
  const map = new Map()
  for (const t of tags.value) {
    const key = t.classificationId || '__none__'
    if (!map.has(key)) {
      map.set(key, { classificationId: key, classificationName: t.classificationName, tags: [] })
    }
    map.get(key).tags.push(t)
  }
  return [...map.values()]
})

onMounted(async () => {
  try {
    tags.value = await tagApi.list() || []
  } catch (e) {
    tags.value = []
  }
})
</script>

<style scoped>
.tag-option {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.tag-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.tag-select-empty {
  color: #9ca3af;
  font-size: 12px;
  padding: 8px 0;
  text-align: center;
}
</style>
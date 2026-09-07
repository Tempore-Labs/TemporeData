<template>
  <template v-for="f in fields" :key="f.key">
    <!-- Asset search (remote) -->
    <el-form-item v-if="f.type === 'assetSearch'" :label="f.label" :prop="f.key">
      <el-select
        :model-value="form[f.key]"
        filterable
        remote
        :remote-method="(k) => onSearch(k, f.key)"
        :loading="props.searching"
        :disabled="!!f.disabled"
        :placeholder="f.placeholder"
        style="width:100%"
        @update:model-value="(v) => (form[f.key] = v)"
      >
        <el-option v-for="r in resultsMap[f.key] || []" :key="r.id" :label="r.name" :value="r.id">
          <span>{{ r.name }}</span>
          <el-tag size="small" class="lg-ff-tag">{{ r.nodeType }}</el-tag>
        </el-option>
      </el-select>
    </el-form-item>

    <!-- Enum (select from list) -->
    <el-form-item v-else-if="f.type === 'enum'" :label="f.label" :prop="f.key">
      <el-select
        :model-value="form[f.key]"
        style="width:100%"
        :disabled="!!f.disabled"
        @update:model-value="(v) => (form[f.key] = v)"
      >
        <el-option v-for="opt in f.enum || []" :key="opt" :label="optionLabel(f, opt)" :value="opt" />
      </el-select>
    </el-form-item>

    <!-- Textarea -->
    <el-form-item v-else-if="f.type === 'textarea'" :label="f.label" :prop="f.key">
      <el-input
        :model-value="form[f.key]"
        type="textarea"
        :rows="f.rows || 2"
        :placeholder="f.placeholder"
        @update:model-value="(v) => (form[f.key] = v)"
      />
    </el-form-item>

    <!-- Single-line input -->
    <el-form-item v-else-if="f.type === 'input'" :label="f.label" :prop="f.key">
      <el-input
        :model-value="form[f.key]"
        :placeholder="f.placeholder"
        @update:model-value="(v) => (form[f.key] = v)"
      />
    </el-form-item>

    <!-- Taggable multi select (allow-create) -->
    <el-form-item v-else-if="f.type === 'taggable'" :label="f.label" :prop="f.key">
      <el-select
        :model-value="form[f.key]"
        multiple
        filterable
        allow-create
        default-first-option
        style="width:100%"
        @update:model-value="(v) => (form[f.key] = v)"
      />
    </el-form-item>
  </template>
</template>

<script setup>
/**
 * Shared field renderer for the write-dialog FormSchema (B2). Given a list of
 * `edgeFormMeta` items (enriched with per-dialog flags) plus the bound form object
 * and a results map, it renders the correct Element Plus control per field type.
 * Column-specific labels (e.g. relation type / priority) come from `labelFn`.
 */
const props = defineProps({
  fields: { type: Array, default: () => [] },
  form: { type: Object, default: () => ({}) },
  resultsMap: { type: Object, default: () => ({}) },
  searching: { type: Boolean, default: false },
  labelFn: { type: Function, default: (f, opt) => opt || '' }
})

const emit = defineEmits(['search'])

function onSearch(keyword, fieldKey) {
  emit('search', { keyword, key: fieldKey })
}

function optionLabel(f, opt) {
  if (typeof props.labelFn === 'function') return props.labelFn(f, opt)
  return opt
}
</script>

<style scoped>
.lg-ff-tag {
  margin-left: 8px;
  transform: scale(0.9);
}
</style>
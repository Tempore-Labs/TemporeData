<template>
  <div class="lg-dlg-bar">
    <el-button v-if="!hideCancel" :disabled="disabled || loading" @click="emit('cancel')">{{ cancelText }}</el-button>
    <el-button
      type="primary"
      :loading="loading"
      :disabled="disabled"
      :aria-disabled="disabled"
      @click="emit('save')"
    >{{ saveText }}</el-button>
  </div>
</template>

<script setup>
/**
 * Standard dialog footer (C-09). Cancel on the left, primary save on the right
 * with a loading state. Used by every write dialog to reduce duplicated markup.
 */
defineProps({
  saveText: { type: String, default: '保存' },
  cancelText: { type: String, default: '取消' },
  loading: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  hideCancel: { type: Boolean, default: false }
})

const emit = defineEmits(['save', 'cancel'])
</script>

<style scoped>
.lg-dlg-bar {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  width: 100%;
}
</style>
<template>
  <div class="editor-page">
    <el-skeleton v-if="!wf" :rows="6" animated />
    <WorkflowCanvas v-else :key="wf.id" :workflow="wf" :name="wf.name" @saved="onSaved" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import WorkflowCanvas from './components/WorkflowCanvas.vue'
import { workflowApi, type WorkflowRes } from '@/api/workflow'

const route = useRoute()
const workflowId = route.params.id as string
const wf = ref<WorkflowRes | null>(null)

function onSaved(res: WorkflowRes) {
  wf.value = res
}

onMounted(async () => {
  wf.value = await workflowApi.get(workflowId)
})
</script>

<style scoped>
.editor-page { height: 100%; }
</style>
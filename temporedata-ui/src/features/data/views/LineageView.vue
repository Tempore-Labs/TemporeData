<template>
  <section class="v2-page v2-lineage">
    <div class="v2-page-head">
      <h2 class="v2-page-title">血缘</h2>
    </div>

    <el-alert v-if="state === 'permission-denied'" type="warning" :closable="false" title="无权限查看血缘" />

    <!-- Reuse the mature legacy lineage entry (LineageV2/Lineage + G6 graph).
         Migration is a thin v2 wrapper — no rewrite of the graph implementation. -->
    <Suspense v-else>
      <template #default>
        <LineageEntry v-if="loaded" />
      </template>
      <template #fallback>
        <div v-loading="true" class="v2-min-h" />
      </template>
    </Suspense>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { defineAsyncComponent } from 'vue'
import { usePageState } from '@/composables/usePageState'
import { hasPerm } from '@/permissions'

const { state, deny } = usePageState()
const loaded = ref(false)

// Async/code-split so the G6 lineage graph (LineageV2/Lineage) is not bundled
// into every data route on first load.
const LineageEntry = defineAsyncComponent(() => import('@/views/LineageEntry.vue'))

onMounted(() => {
  if (!hasPerm('data:read')) { deny(); return }
  loaded.value = true
})
</script>

<style scoped>
.v2-page-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.v2-page-title { font-size: 18px; margin: 0; }
.v2-lineage { min-height: calc(100vh - 160px); }
.v2-min-h { min-height: 120px; }
</style>
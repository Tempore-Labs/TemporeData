<template>
  <div class="lineage-graph" ref="containerRef">
    <div v-if="!nodes.length" class="empty-hint">暂无血缘关系数据</div>
    <v-chart v-else :option="chartOption" autoresize class="lineage-chart" />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { GraphChart } from 'echarts/charts'
import { TooltipComponent } from 'echarts/components'

use([CanvasRenderer, GraphChart, TooltipComponent])

const props = defineProps({
  nodes: { type: Array, default: () => [] },
  edges: { type: Array, default: () => [] },
  centerTableId: { type: String, default: '' },
  centerTableName: { type: String, default: '' }
})

const containerRef = ref(null)

const chartOption = computed(() => {
  const graphNodes = []
  const graphLinks = []
  const nodeSet = new Set()

  // Add center node
  graphNodes.push({
    id: props.centerTableId || 'center',
    name: props.centerTableName || '当前表',
    symbolSize: 48,
    itemStyle: { color: '#1a6ff5' },
    label: { show: true, fontSize: 13, fontWeight: 'bold' }
  })
  nodeSet.add(props.centerTableId || 'center')

  // Add upstream/downstream nodes
  const colors = {
    SYNC: '#10b981', INGESTION: '#f59e0b', WORKFLOW: '#8b5cf6',
    QUERY: '#3b82f6', QUALITY: '#ef4444', METADATA: '#6b7280'
  }
  props.nodes.forEach(n => {
    if (!nodeSet.has(n.tableId)) {
      nodeSet.add(n.tableId)
      graphNodes.push({
        id: n.tableId,
        name: n.tableName,
        symbolSize: 36,
        itemStyle: { color: colors[n.relationType] || '#6b7280' },
        label: { show: true, fontSize: 11 },
        category: n.relationType
      })
    }
  })

  props.edges.forEach(e => {
    graphLinks.push({
      source: e.sourceTableId || 'center',
      target: e.targetTableId || 'center',
      label: { show: true, formatter: e.relationType, fontSize: 10 }
    })
  })

  return {
    tooltip: {
      trigger: 'item',
      formatter: (p) => p.dataType === 'node' ? `${p.name}` : `${p.data.source} → ${p.data.target}`
    },
    series: [{
      type: 'graph',
      layout: 'force',
      roam: true,
      draggable: true,
      force: { repulsion: 300, edgeLength: [150, 250] },
      data: graphNodes,
      links: graphLinks,
      lineStyle: { color: '#94a3b8', curveness: 0.3, width: 1.5 }
    }]
  }
})
</script>

<style scoped>
.lineage-graph { width: 100%; height: 500px; border: 1px solid #e5e7eb; border-radius: 8px; background: #fafbfc; overflow: hidden; }
.empty-hint { display: flex; align-items: center; justify-content: center; height: 100%; color: #9ca3af; font-size: 14px; }
.lineage-chart { width: 100%; height: 100%; }
</style>
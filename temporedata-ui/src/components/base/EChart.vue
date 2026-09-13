<template>
  <div ref="el" class="echart" :style="{ height: height + 'px', width: '100%' }" />
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

const props = withDefaults(
  defineProps<{
    option: echarts.EChartsOption
    height?: number
    autoresize?: boolean
  }>(),
  { height: 220, autoresize: true },
)

const el = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null
let ro: ResizeObserver | null = null

function render() {
  if (!chart && el.value) chart = echarts.init(el.value)
  chart?.setOption(props.option, true)
}

onMounted(() => {
  render()
  if (props.autoresize && el.value) {
    ro = new ResizeObserver(() => chart?.resize())
    ro.observe(el.value)
  }
})

watch(() => props.option, render, { deep: true })

onBeforeUnmount(() => {
  ro?.disconnect()
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.echart {
  min-width: 0;
}
</style>

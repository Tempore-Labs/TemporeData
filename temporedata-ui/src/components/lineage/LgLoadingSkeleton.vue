<template>
  <div class="lg-skeleton" role="status" aria-label="Loading graph skeleton">
    <div class="sk-nodes">
      <span v-for="i in 6" :key="i" class="sk-node" :style="nodeStyle(i)" />
    </div>
    <svg class="sk-edges" :viewBox="viewBox" aria-hidden="true">
      <line v-for="(l, idx) in lines" :key="idx" :x1="l[0]" :y1="l[1]" :x2="l[2]" :y2="l[3]" />
    </svg>
  </div>
</template>

<script setup>
/**
 * CSS/SVG placeholder shown in the canvas area while the graph is loading.
 * Renders 6 elliptical "node" placeholders connected by 10 grey skeleton lines.
 */
const viewBox = '0 0 120 100'

const positions = [
  [50, 18, 14, 10],
  [16, 40, 15, 10],
  [84, 38, 14, 10],
  [30, 66, 15, 10],
  [72, 68, 14, 10],
  [84, 12, 13, 9]
]

function nodeStyle(i) {
  const [x, y, w, h] = positions[i - 1]
  return {
    left: `${x}%`,
    top: `${y}%`,
    width: `${w * 1.2}px`,
    height: `${h * 1.3}px`
  }
}

// 10 grey connector lines across the skeleton viewport.
const lines = [
  [26, 45, 46, 25],
  [58, 23, 78, 43],
  [24, 74, 58, 62],
  [62, 72, 86, 50],
  [48, 28, 22, 46],
  [78, 40, 66, 64],
  [20, 44, 34, 62],
  [62, 62, 80, 42],
  [42, 27, 60, 66],
  [88, 16, 74, 34]
]
</script>

<style scoped>
.lg-skeleton {
  position: relative;
  width: 100%;
  height: 100%;
  background:
    linear-gradient(rgba(148, 163, 184, 0.14) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.14) 1px, transparent 1px),
    var(--lg-bg-canvas, #F5F7FA);
  background-size: 24px 24px, 24px 24px, 100% 100%;
  overflow: hidden;
}
.sk-nodes {
  position: absolute;
  inset: 0;
}
.sk-node {
  position: absolute;
  border-radius: 5px;
  background: linear-gradient(100deg, #e2e8f0 30%, #f1f5f9 50%, #e2e8f0 70%);
  background-size: 200% 100%;
  animation: lg-shimmer 1.4s linear infinite;
}
.sk-edges {
  position: absolute;
  inset: 8%;
  width: 84%;
  height: 84%;
}
.sk-edges line {
  stroke: #cbd5e1;
  stroke-width: 1.5;
  stroke-dasharray: 3 5;
  animation: lg-flow 1.6s linear infinite;
}
@keyframes lg-shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
@keyframes lg-flow {
  0% { stroke-dashoffset: 0; }
  100% { stroke-dashoffset: -16; }
}
</style>
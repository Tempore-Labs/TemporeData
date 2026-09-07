<template>
  <div class="dag-node" :class="['node-' + (data.type || 'sql').toLowerCase(), statusClass]">
    <!-- Status indicator dot -->
    <div class="status-dot" v-if="data.status" :class="'status-' + data.status.toLowerCase()" :title="data.status" />

    <div class="node-header">
      <el-icon class="node-icon" :size="14">
        <component :is="nodeIcon" />
      </el-icon>
      <span class="node-type-badge">{{ data.type || 'SQL' }}</span>
      <span class="node-priority" v-if="data.priority && data.priority !== 'MEDIUM'">
        {{ priorityLabel }}
      </span>
    </div>
    <div class="node-body">
      <span class="node-name">{{ data.label || '未命名' }}</span>
    </div>
    <div class="node-footer">
      <span class="node-ds" v-if="data.datasourceName">{{ data.datasourceName }}</span>
      <span class="node-ds" v-else-if="data.datasourceId">数据源</span>
      <span class="node-config" v-if="data.retryCount > 0">
        <el-icon :size="10"><RefreshRight /></el-icon> {{ data.retryCount }}
      </span>
      <span class="node-config" v-if="data.timeoutSeconds">
        <el-icon :size="10"><Clock /></el-icon> {{ data.timeoutSeconds }}s
      </span>
    </div>
    <Handle type="target" :position="Position.Top" />
    <Handle type="source" :position="Position.Bottom" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Handle, Position } from '@vue-flow/core'
import { Collection, Cpu, Document, RefreshRight, Clock, StarFilled, Connection, Link, Share, Switch, Message, CircleCheck } from '@element-plus/icons-vue'

const props = defineProps({
  data: { type: Object, required: true }
})

const nodeIcon = computed(() => {
  const map = {
    sql: Document,
    shell: Cpu,
    python: Cpu,
    spark_sql: StarFilled,
    datax: Connection,
    http: Link,
    sub_workflow: Share,
    condition: Switch,
    email: Message,
    dependency: Clock,
    data_quality: CircleCheck
  }
  return map[(props.data.type || 'sql').toLowerCase()] || Document
})

const statusClass = computed(() => {
  if (props.data.status === 'RUNNING') return 'node-running'
  if (props.data.status === 'SUCCESS') return 'node-success'
  if (props.data.status === 'FAILED') return 'node-failed'
  if (props.data.status === 'SKIPPED') return 'node-skipped'
  return ''
})

const priorityLabel = computed(() => {
  const map = { HIGHEST: '最高', HIGH: '高', LOW: '低', LOWEST: '最低' }
  return map[props.data.priority] || ''
})
</script>

<style scoped>
.dag-node {
  background: #fff;
  border: 2px solid #d1d5db;
  border-radius: 8px;
  padding: 8px 14px;
  min-width: 150px;
  font-size: 12px;
  box-shadow: 0 1px 3px rgba(0,0,0,.08);
  transition: border-color .2s, box-shadow .2s;
  cursor: pointer;
  position: relative;
}
.dag-node:hover { border-color: #3b82f6; box-shadow: 0 2px 8px rgba(59,130,246,.2); }
.dag-node.selected { border-color: #3b82f6; box-shadow: 0 0 0 2px rgba(59,130,246,.25); }

/* Status colors */
.node-running { border-color: #f59e0b; animation: pulse 1.5s infinite; }
.node-success { border-color: #10b981; }
.node-failed { border-color: #ef4444; }
.node-skipped { border-color: #d1d5db; opacity: .6; }

@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(245,158,11,.4); }
  50% { box-shadow: 0 0 0 6px rgba(245,158,11,0); }
}

/* Status dot */
.status-dot {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid #fff;
  box-shadow: 0 0 0 1px rgba(0,0,0,.1);
}
.status-running { background: #f59e0b; }
.status-success { background: #10b981; }
.status-failed { background: #ef4444; }
.status-skipped { background: #9ca3af; }

.node-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}
.node-icon { color: #6b7280; }
.node-type-badge {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
  background: #f3f4f6;
  color: #6b7280;
  font-weight: 600;
}
.node-sql .node-type-badge { background: #dbeafe; color: #2563eb; }
.node-shell .node-type-badge { background: #fef3c7; color: #d97706; }
.node-python .node-type-badge { background: #d1fae5; color: #059669; }
.node-spark_sql .node-type-badge { background: #fce7f3; color: #db2777; }
.node-datax .node-type-badge { background: #e0e7ff; color: #4f46e5; }
.node-http .node-type-badge { background: #cffafe; color: #0891b2; }
.node-sub_workflow .node-type-badge { background: #f3e8ff; color: #7c3aed; }
.node-condition .node-type-badge { background: #fef9c3; color: #ca8a04; }
.node-email .node-type-badge { background: #dcfce7; color: #16a34a; }
.node-dependency .node-type-badge { background: #fee2e2; color: #dc2626; }
.node-data_quality .node-type-badge { background: #e0f2fe; color: #0284c7; }

/* Left border color for each type */
.node-sql { border-left: 3px solid #2563eb; }
.node-shell { border-left: 3px solid #d97706; }
.node-python { border-left: 3px solid #059669; }
.node-spark_sql { border-left: 3px solid #db2777; }
.node-datax { border-left: 3px solid #4f46e5; }
.node-http { border-left: 3px solid #0891b2; }
.node-sub_workflow { border-left: 3px solid #7c3aed; }
.node-condition { border-left: 3px solid #ca8a04; }
.node-email { border-left: 3px solid #16a34a; }
.node-dependency { border-left: 3px solid #dc2626; }
.node-data_quality { border-left: 3px solid #0284c7; }

.node-priority {
  font-size: 9px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #fee2e2;
  color: #dc2626;
  font-weight: 600;
  margin-left: auto;
}

.node-body { margin-bottom: 4px; }
.node-name {
  font-weight: 600;
  color: #1f2937;
  font-size: 13px;
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 140px;
}

.node-footer {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 10px;
  color: #9ca3af;
}
.node-ds { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 100px; }
.node-config {
  display: flex;
  align-items: center;
  gap: 1px;
  color: #6b7280;
  background: #f3f4f6;
  padding: 1px 4px;
  border-radius: 3px;
}
</style>
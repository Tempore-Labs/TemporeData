<template>
  <div class="page">
    <PageHeader title="RCA-Agent" subtitle="根因分析智能体：从事件到行动计划">
      <template #actions>
        <el-button size="small" type="primary" :disabled="!selectedId" @click="analyze">分析根因</el-button>
      </template>
    </PageHeader>

    <div class="rca-layout">
      <!-- Incident list -->
      <Card class="incident-pane">
        <div class="panel-title">事件列表（按状态）</div>
        <el-select v-model="status" size="small" style="width: 100%; margin-bottom: 12px" @change="load">
          <el-option v-for="s in ['OPEN', 'INVESTIGATING', 'RESOLVED', 'CLOSED']" :key="s" :label="s" :value="s" />
        </el-select>
        <el-skeleton v-if="loading" :rows="5" animated />
        <div v-else class="incident-list">
          <button
            v-for="inc in incidents"
            :key="inc.id"
            class="incident-item"
            :class="{ active: selectedId === inc.id }"
            @click="selectedId = inc.id"
          >
            <div class="inc-title">#{{ inc.id }} {{ inc.title }}</div>
            <div class="inc-meta">
              <StatusBadge :status="inc.severity === 'CRITICAL' ? 'failed' : inc.severity === 'HIGH' ? 'failed' : 'warning'" :text="inc.severity || '—'" />
              <span class="mono">{{ fmtTime(inc.createdAt) }}</span>
            </div>
          </button>
          <el-empty v-if="incidents.length === 0" description="暂无事件" :image-size="60" />
        </div>
      </Card>

      <!-- Finding panel -->
      <Card class="finding-pane">
        <div class="panel-title">根因分析</div>
        <el-skeleton v-if="analyzing" :rows="8" animated />
        <template v-else-if="plan">
          <el-alert type="info" :closable="false">
            <template #title>根因类型: {{ plan.finding.causeType }} · 置信度 {{ (plan.finding.confidence * 100).toFixed(0) }}%</template>
            <p class="hypo">{{ plan.finding.hypothesis }}</p>
          </el-alert>

          <div class="block-title">行动计划 ({{ plan.actions.length }})</div>
          <div class="action-list">
            <div v-for="(a, i) in plan.actions" :key="i" class="action-item">
              <div class="action-head">
                <span class="action-title">{{ a.title }}</span>
                <StatusBadge
                  :status="a.allowed ? 'pass' : a.requiresApproval ? 'warning' : 'failed'"
                  :text="a.allowed ? '自动可执行' : a.requiresApproval ? '需审批' : '禁止'"
                />
              </div>
              <div class="action-cmd mono">{{ a.suggestedCommand }}</div>
              <div class="action-reason">{{ a.reason }}</div>
            </div>
          </div>

          <div class="apply-row">
            <el-button type="primary" :loading="applying" @click="apply(false)">应用自动项</el-button>
            <el-button v-if="plan.actions.some((a) => a.requiresApproval)" type="warning" :loading="applying" @click="apply(true)">批准并全部应用</el-button>
          </div>
        </template>
        <div v-else class="finding-empty">
          <div class="fe-icon"><el-icon :size="28"><MagicStick /></el-icon></div>
          <div>选择左侧事件，点击「分析根因」生成行动计划</div>
        </div>
      </Card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useQueryClient } from '@tanstack/vue-query'
import PageHeader from '@/components/base/PageHeader.vue'
import Card from '@/components/base/Card.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { incidentApi } from '@/api/incident'
import { rcaApi } from '@/api/rca'
import type { IncidentEntity } from '@/types/incident'
import type { ActionPlan } from '@/types/rca'

const queryClient = useQueryClient()
const incidents = ref<IncidentEntity[]>([])
const loading = ref(false)
const status = ref('OPEN')
const selectedId = ref<number | null>(null)

const plan = ref<ActionPlan | null>(null)
const analyzing = ref(false)
const applying = ref(false)

async function load() {
  loading.value = true
  try {
    incidents.value = await incidentApi.byStatus(status.value)
  } finally {
    loading.value = false
  }
}
load()

function analyze() {
  if (selectedId.value == null) return
  analyzing.value = true
  plan.value = null
  rcaApi
    .analyze(selectedId.value)
    .then((p) => {
      plan.value = p
      ElMessage.success('分析完成')
    })
    .finally(() => (analyzing.value = false))
}

async function apply(approve: boolean) {
  if (selectedId.value == null) return
  applying.value = true
  try {
    const res = await rcaApi.apply(selectedId.value, approve)
    ElMessage.success(`已应用 ${res.applied} 项，待审批 ${res.needsApproval} 项，状态 ${res.status}`)
    queryClient.invalidateQueries({ queryKey: ['incident'] })
    await load()
  } finally {
    applying.value = false
  }
}

function fmtTime(t?: string) {
  return t ? t.replace('T', ' ').slice(0, 19) : '—'
}
</script>

<style scoped>
.page {
  padding: 24px;
}
.rca-layout {
  display: grid;
  grid-template-columns: 340px 1fr;
  gap: 16px;
  align-items: start;
}
.incident-pane,
.finding-pane {
  padding: 20px;
}
.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--td-text-1);
  margin-bottom: 14px;
}
.incident-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 560px;
  overflow: auto;
}
.incident-item {
  text-align: left;
  padding: 10px 12px;
  border: 1px solid var(--td-border-light);
  border-radius: 10px;
  background: var(--td-surface);
  cursor: pointer;
  transition: border-color 0.15s;
}
.incident-item:hover {
  border-color: var(--td-primary-border);
}
.incident-item.active {
  border-color: var(--td-primary);
  background: var(--td-primary-soft);
}
.inc-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--td-text-1);
  margin-bottom: 6px;
}
.inc-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.mono {
  font-family: var(--td-font-mono);
  font-size: 11px;
  color: var(--td-text-4);
}
.block-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--td-text-2);
  margin: 16px 0 10px;
}
.hypo {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--td-text-2);
}
.action-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 360px;
  overflow: auto;
}
.action-item {
  padding: 12px 14px;
  border: 1px solid var(--td-border);
  border-radius: 10px;
  background: var(--td-bg);
}
.action-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.action-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--td-text-1);
}
.action-cmd {
  font-size: 11px;
  color: var(--td-primary);
  margin-top: 6px;
  font-family: var(--td-font-mono);
}
.action-reason {
  font-size: 11px;
  color: var(--td-text-4);
  margin-top: 4px;
}
.apply-row {
  display: flex;
  gap: 8px;
  margin-top: 16px;
}
.finding-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  color: var(--td-text-3);
  font-size: 13px;
  text-align: center;
}
.fe-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: var(--td-primary-soft);
  color: var(--td-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 14px;
}
</style>

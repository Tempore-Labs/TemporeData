<template>
  <div class="page">
    <PageHeader title="AI 门控" subtitle="AI 生成命令的安全策略评估（RBAC + 数据分级 + 风险阻断）">
      <template #actions>
        <el-button size="small" type="primary" @click="doCheck">执行评估</el-button>
      </template>
    </PageHeader>

    <div class="gate-layout">
      <!-- Request panel -->
      <Card class="req-card">
        <div class="panel-title">命令输入</div>
        <el-form :model="form" label-width="100px">
          <el-form-item label="命令类型">
            <el-select v-model="form.type" style="width: 100%">
              <el-option v-for="t in ['QUERY', 'EXPORT', 'WRITE', 'DROP', 'ALTER', 'GRANT', 'OTHER']" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item>
          <el-form-item label="执行主体">
            <el-input v-model="form.principal" placeholder="如 ai-assistant / admin" />
          </el-form-item>
          <el-form-item label="目标数据集">
            <el-input-number v-model="form.targetDatasetId" :min="1" style="width: 100%" />
          </el-form-item>
          <el-form-item label="安全级别">
            <el-select v-model="form.securityLevel" style="width: 100%">
              <el-option v-for="l in ['L1', 'L2', 'L3', 'L4']" :key="l" :label="`L${l.slice(1)} ${levelLabel(l)}`" :value="l" />
            </el-select>
          </el-form-item>
          <el-form-item label="命令内容">
            <el-input v-model="form.command" type="textarea" :rows="4" placeholder="如 DELETE FROM ods_order_detail WHERE ..." />
          </el-form-item>
          <el-form-item label="自动批准">
            <el-switch v-model="form.autoApproved" />
          </el-form-item>
        </el-form>
      </Card>

      <!-- Decision panel -->
      <Card class="decision-card">
        <div class="panel-title">评估结果</div>
        <template v-if="decision">
          <div class="decision-banner" :class="decision.allowed ? 'ok' : decision.requiresApproval ? 'warn' : 'deny'">
            <div class="decision-icon">
              <el-icon :size="26">
                <CircleCheck v-if="decision.allowed" />
                <Warning v-else-if="decision.requiresApproval" />
                <CircleClose v-else />
              </el-icon>
            </div>
            <div class="decision-text">
              <div class="decision-title">
                {{ decision.allowed ? '放行' : decision.requiresApproval ? '需要人工审批' : '已阻断' }}
              </div>
              <div class="decision-risk">风险等级: {{ decision.risk }}</div>
            </div>
          </div>
          <el-descriptions :column="1" border size="small" class="decision-meta">
            <el-descriptions-item label="是否放行">
              <StatusBadge :status="decision.allowed ? 'pass' : 'failed'" :text="decision.allowed ? 'ALLOWED' : 'DENIED'" />
            </el-descriptions-item>
            <el-descriptions-item label="需审批">
              <StatusBadge :status="decision.requiresApproval ? 'warning' : 'pass'" :text="decision.requiresApproval ? 'YES' : 'NO'" />
            </el-descriptions-item>
            <el-descriptions-item label="判定原因">{{ decision.reason }}</el-descriptions-item>
          </el-descriptions>
        </template>
        <el-empty v-else description="填写左侧命令并点击「执行评估」" :image-size="72" />
      </Card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/base/PageHeader.vue'
import Card from '@/components/base/Card.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { aiSafetyApi } from '@/api/aiSafety'
import type { AiCommandRequest, AiPolicyDecision } from '@/types/aiSafety'

const form = reactive<AiCommandRequest>({
  type: 'QUERY',
  principal: 'ai-assistant',
  targetDatasetId: 1,
  target: '',
  securityLevel: 'L2',
  command: '',
  autoApproved: false,
})

const decision = ref<AiPolicyDecision | null>(null)
const checking = ref(false)

async function doCheck() {
  checking.value = true
  try {
    decision.value = await aiSafetyApi.check(form)
    ElMessage.success('评估完成')
  } catch {
    /* http layer toasts */
  } finally {
    checking.value = false
  }
}

function levelLabel(l: string) {
  return { L1: '公开', L2: '内部', L3: '敏感', L4: '机密' }[l] || ''
}
</script>

<style scoped>
.page {
  padding: 24px;
}
.gate-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  align-items: start;
}
.req-card,
.decision-card {
  padding: 20px;
}
.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--td-text-1);
  margin-bottom: 16px;
}
.decision-banner {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 16px;
}
.decision-banner.ok {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  color: #065f46;
}
.decision-banner.warn {
  background: #fefce8;
  border: 1px solid #fde68a;
  color: #92400e;
}
.decision-banner.deny {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #991b1b;
}
.decision-icon {
  color: inherit;
}
.decision-title {
  font-size: 18px;
  font-weight: 700;
}
.decision-risk {
  font-size: 12px;
  margin-top: 4px;
  opacity: 0.8;
}
.decision-meta {
  margin-top: 8px;
}
</style>

<template>
  <div class="page">
    <PageHeader title="网关控制台" subtitle="企业数据服务网关 - AppKey 认证与令牌桶限流验证" />

    <div class="gateway-grid">
      <!-- AppKey 校验 -->
      <Card class="gw-card">
        <div class="panel-title">AppKey 认证</div>
        <el-alert type="info" :closable="false" show-icon title="当前为演示模式：非空 AppKey 即通过（未配置严格 key）" style="margin-bottom: 16px" />
        <div class="probe-row">
          <el-input v-model="appKey" placeholder="输入 AppKey，如 app-001" size="small" style="flex: 1">
            <template #prefix><el-icon><Key /></el-icon></template>
          </el-input>
          <el-button size="small" type="primary" :loading="checking" @click="doCheck">校验</el-button>
        </div>

        <div v-if="checkResult" class="result-banner" :class="checkResult.http < 400 ? 'ok' : 'deny'">
          <el-icon :size="18"><CircleCheck v-if="checkResult.http < 400" /><CircleClose v-else /></el-icon>
          <div>
            <div class="res-title">
              {{ checkResult.http < 400 ? '认证通过' : `认证失败 (HTTP ${checkResult.http})` }}
            </div>
            <div class="res-sub mono">code={{ checkResult.code }} · msg={{ checkResult.msg }}</div>
          </div>
        </div>
      </Card>

      <!-- 速率限流沙盒 -->
      <Card class="gw-card">
        <div class="panel-title">令牌桶限流沙盒</div>
        <el-alert type="info" :closable="false" show-icon title="每按一次探测消耗 1 令牌；burst 用尽后返回 429，随固定速率自动补充" style="margin-bottom: 16px" />
        <div class="probe-row">
          <el-input v-model="consumer" placeholder="消费者标识，如 demo-app" size="small" style="flex: 1.4">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
          <el-select v-model="burst" size="small" style="width: 110px">
            <el-option v-for="b in [1, 2, 3, 5, 10]" :key="b" :label="`burst=${b}`" :value="b" />
          </el-select>
          <el-button size="small" type="primary" :loading="probing" @click="probeOnce">探测一次</el-button>
          <el-button size="small" :disabled="probing" @click="probeBurst">连打 {{ burst }} 次</el-button>
        </div>

        <div v-if="history.length" class="probe-history">
          <div v-for="(h, i) in history" :key="i" class="probe-item">
            <StatusBadge :status="h.code === 200 ? 'success' : 'failed'" :text="h.code === 200 ? '200 放行' : h.code + ' 限流'" />
            <span class="mono res-remaining">剩余令牌 ≈ {{ h.remaining != null ? h.remaining.toFixed(1) : '—' }}</span>
          </div>
        </div>
        <div v-else class="gw-empty">点击「探测一次」或「连打 N 次」观察令牌消耗与 429 触发</div>
      </Card>
    </div>

    <Card class="gw-card explain-card">
      <div class="panel-title">说明</div>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="算法">令牌桶（Token Bucket）· 每消费者独立桶 · 线程安全</el-descriptions-item>
        <el-descriptions-item label="默认参数">
          持续速率 <span class="mono">5 tokens/s</span> · burst 默认 <span class="mono">3</span>（可调 1/2/3/5/10）
        </el-descriptions-item>
        <el-descriptions-item label="ApiKey 调用链">
          外部调用 <span class="mono">/api/services/{id}/invoke</span> 需 <span class="mono">X-API-Key + X-Timestamp + X-Nonce + X-Signature</span>（HMAC-SHA256）
        </el-descriptions-item>
      </el-descriptions>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/base/PageHeader.vue'
import Card from '@/components/base/Card.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { gatewayApi, type GatewayProbe } from '@/api/gateway'

const appKey = ref('app-001')
const checking = ref(false)
const checkResult = ref<GatewayProbe | null>(null)

const consumer = ref('demo-app')
const burst = ref(3)
const probing = ref(false)
const history = ref<GatewayProbe[]>([])

async function doCheck() {
  if (!appKey.value.trim()) {
    ElMessage.warning('请先输入 AppKey')
    return
  }
  checking.value = true
  try {
    checkResult.value = await gatewayApi.check(appKey.value)
    if (checkResult.value.http >= 400) ElMessage.warning(`认证失败 (${checkResult.value.http})`)
  } finally {
    checking.value = false
  }
}

async function probeOnce() {
  if (!consumer.value.trim()) {
    ElMessage.warning('请先输入消费者标识')
    return
  }
  probing.value = true
  try {
    const res = await gatewayApi.ratelimit(consumer.value, burst.value)
    history.value = [res, ...history.value].slice(0, 30)
    if (res.code === 429) ElMessage.warning('达到速率上限 (429)')
  } finally {
    probing.value = false
  }
}

async function probeBurst() {
  probing.value = true
  const snapshot: GatewayProbe[] = []
  for (let i = 0; i < burst.value; i++) {
    snapshot.push(await gatewayApi.ratelimit(consumer.value, burst.value))
  }
  history.value = [...snapshot.reverse(), ...history.value].slice(0, 40)
  probing.value = false
  const limited = snapshot.filter((s) => s.code === 429).length
  ElMessage[limited ? 'warning' : 'success'](`本轮 ${burst.value} 次中 ${limited} 次被限流`)
}
</script>

<style scoped>
.page {
  padding: 24px;
}
.gateway-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
  align-items: start;
}
.gw-card {
  padding: 20px;
}
.explain-card {
  margin-top: 4px;
}
.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--td-text-1);
  margin-bottom: 16px;
}
.probe-row {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.result-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-radius: 10px;
  border: 1px solid;
}
.result-banner.ok {
  background: #f0fdf4;
  border-color: #bbf7d0;
  color: #065f46;
}
.result-banner.deny {
  background: #fef2f2;
  border-color: #fecaca;
  color: #991b1b;
}
.res-title {
  font-size: 14px;
  font-weight: 600;
}
.res-sub {
  font-size: 11px;
  margin-top: 2px;
  opacity: 0.8;
}
.probe-history {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 240px;
  overflow: auto;
}
.probe-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  border: 1px solid var(--td-border-light);
  border-radius: 8px;
  background: var(--td-bg);
}
.res-remaining {
  font-size: 11px;
  color: var(--td-text-3);
}
.gw-empty {
  padding: 24px;
  text-align: center;
  color: var(--td-text-4);
  font-size: 12px;
}
.mono {
  font-family: var(--td-font-mono);
}
</style>
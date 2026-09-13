<template>
  <div class="sch">
    <el-form label-width="130px" size="small" class="sch-form">
      <el-form-item label="启用调度">
        <el-switch v-model="enabled" />
      </el-form-item>
      <el-form-item label="运行周期 (Cron)">
        <el-input v-model="cron" placeholder="如 0 8 * * *" class="sch-cron" />
        <div class="sch-mini">
          <span v-for="(seg, i) in cron.split(' ')" :key="i" class="sch-seg">{{ seg || '?' }}</span>
        </div>
      </el-form-item>
      <el-form-item label="可视化构建">
        <div class="sch-builder">
          <el-select v-model="freq" style="width: 110px" @change="applyPreset">
            <el-option v-for="f in ['分','时','日','周','月']" :key="f" :value="f" />
          </el-select>
          <el-input-number v-model="freqEvery" :min="1" :max="23" size="small" style="width: 90px" @change="applyPreset" />
          <span class="sch-hint">每 {{ freqEvery }} {{ freq }}</span>
        </div>
      </el-form-item>
      <el-form-item label="并发控制">
        <el-select v-model="policy" style="width: 200px">
          <el-option label="串行（等待上一周期完成）" value="SERIAL" />
          <el-option label="并行执行" value="PARALLEL" />
          <el-option label="直接抛弃本周期" value="DROP" />
        </el-select>
      </el-form-item>
      <el-form-item label="Missfire 策略">
        <el-select v-model="missfire" style="width: 200px">
          <el-option label="跳过本次" value="SKIP" />
          <el-option label="立即全量补发" value="BACKFILL" />
        </el-select>
      </el-form-item>
      <el-form-item label="未来触发预览">
        <ol v-if="nextTimes.length" class="sch-preview">
          <li v-for="t in nextTimes" :key="t" class="mono">{{ t }}</li>
        </ol>
        <span v-else class="sch-hint">（无法解析当前 cron）</span>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="small" :loading="saving" @click="save">保存调度配置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { workflowApi, type WorkflowRes } from '@/api/workflow'

const props = defineProps<{ workflow?: WorkflowRes | null }>()

const cron = ref('0 8 * * *')
const enabled = ref(false)
const policy = ref('SERIAL')
const missfire = ref('SKIP')
const freq = ref('日')
const freqEvery = ref(1)
const saving = ref(false)
const nextTimes = ref<string[]>([])

watch(
  () => props.workflow,
  (wf) => {
    if (!wf) return
    cron.value = wf.cronExpression || '0 8 * * *'
    enabled.value = Boolean((wf as WorkflowRes & { scheduleEnabled?: boolean }).scheduleEnabled)
    policy.value = wf.schedulePolicy || 'SERIAL'
    missfire.value = wf.scheduleMissfire || 'SKIP'
    updateNext()
  },
  { immediate: true },
)

function applyPreset() {
  const s = String(freqEvery.value || 1)
  switch (freq.value) {
    case '分': cron.value = `*/${s} * * * *`; break
    case '时': cron.value = `0 */${s} * * *`; break
    case '日': cron.value = `0 8 */${s} * *`; break
    case '周': cron.value = `0 8 * * ${Math.min(Number(s), 7) - 1}`; break
    case '月': cron.value = `0 8 1 * *`; break
  }
  updateNext()
}

// ---- best-effort cron → future N timestamps (5-field). ----//
type Seq = { from: number; step?: number; to?: number; any?: boolean }[]

function parseSeq(f: string): Seq {
  if (f === '*') return []
  return f.split(',').map((p) => {
    if (p.includes('/')) {
      const [base, step] = p.split('/')
      if (base === '*') return { from: 0, step: Number(step), any: true }
      return { from: Number(base), step: Number(step) }
    }
    if (p.includes('-')) {
      const [a, b] = p.split('-').map(Number)
      return { from: a, to: b }
    }
    const n = Number(p)
    return { from: n, to: n }
  })
}

function sm(val: number, seq: Seq): boolean {
  if (!seq.length) return true
  return seq.some((s) => {
    if (s.any) return val % s.step! === 0
    if (s.step) return val >= s.from && (val - s.from) % s.step === 0
    return val >= s.from && val <= s.to!
  })
}

function cronMatch(fields: Seq[], d: Date): boolean {
  const dom = fields[2]
  const dow = fields[4]
  const domOk = sm(d.getDate(), dom)
  const dowOk = sm(d.getDay(), dow)
  const domStar = dom.length === 0
  const dowStar = dow.length === 0
  const dayOk = (domStar && dowStar) || (domStar ? dowOk : dowStar ? domOk : domOk || dowOk)
  return sm(d.getMinutes(), fields[0]) && sm(d.getHours(), fields[1]) && dayOk && sm(d.getMonth() + 1, fields[3])
}

function updateNext() {
  let fields: Seq[]
  try {
    fields = cron.value.split(/\s+/).map(parseSeq)
  } catch {
    nextTimes.value = []
    return
  }
  if (fields.length !== 5) { nextTimes.value = []; return }
  const out: string[] = []
  const probe = new Date(Date.now() + 60000)
  let guard = 0
  while (out.length < 5 && guard < 300000) {
    if (cronMatch(fields, probe)) {
      out.push(probe.toLocaleString('zh-CN', { hour12: false, month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }))
    }
    probe.setMinutes(probe.getMinutes() + 1)
    guard++
  }
  nextTimes.value = out
}

watch(cron, updateNext)
updateNext()

async function save() {
  if (!props.workflow?.id) { ElMessage.warning('请先保存工作流'); return }
  saving.value = true
  try {
    await workflowApi.schedule(props.workflow.id, cron.value, enabled.value, {
      schedulePolicy: policy.value,
      scheduleMissfire: missfire.value,
    })
    ElMessage.success('调度配置已保存')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.sch { max-width: 720px; padding: 20px; }
.sch-cron { width: 220px; }
.sch-mini { display: inline-flex; gap: 4px; margin-left: 10px; font-family: var(--td-font-mono); }
.sch-seg { background: var(--el-fill-color); border-radius: 4px; padding: 2px 6px; font-size: 12px; }
.sch-builder { display: flex; align-items: center; gap: 8px; }
.sch-hint { color: var(--el-text-color-secondary); }
.sch-preview { margin: 0; padding-left: 18px; color: var(--el-text-color-secondary); }
.mono { font-family: var(--td-font-mono); }
</style>
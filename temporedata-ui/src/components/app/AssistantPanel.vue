<template>
  <div class="ai-drawer-mask" v-if="assistant.open" @click.self="assistant.hide()">
    <aside class="ai-drawer" @click.stop>
      <header class="ai-head">
        <div class="ai-title">
          <el-icon><MagicStick /></el-icon>
          <span>轻舟智助 · AI Copilot</span>
        </div>
        <div class="ai-actions">
          <el-dropdown trigger="click" @command="onSessionCommand">
            <button class="sess-btn" title="会话记录">
              <el-icon :size="15"><Clock /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  v-for="s in sessions"
                  :key="s.sessionId"
                  :command="s"
                >
                  <span class="sess-item">
                    <span class="sess-title">{{ s.title || '未命名会话' }}</span>
                    <el-icon class="sess-del" @click.stop="deleteSession(s)"><Delete /></el-icon>
                  </span>
                </el-dropdown-item>
                <el-dropdown-item v-if="!sessions.length" disabled>暂无历史会话</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-dropdown trigger="click" @command="onConfigCommand">
            <button class="sess-btn" title="模型配置">
              <el-icon :size="15"><Setting /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="config">模型配置</el-dropdown-item>
                <el-dropdown-item command="new">新会话</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <button class="sess-btn" title="关闭" @click="assistant.hide()">
            <el-icon :size="15"><Close /></el-icon>
          </button>
        </div>
      </header>

      <!-- messages -->
      <div ref="bodyRef" class="ai-body">
        <div v-if="!messages.length" class="ai-empty">
          <div class="empty-icon">🧊</div>
          <p>你好，我是轻舟智助。</p>
          <p class="empty-sub">可以帮你检索数据、分析失败、检查质量与血缘。</p>
          <div class="quick-qs">
            <button v-for="q in quickQuestions" :key="q" class="quick" @click="send(q)">{{ q }}</button>
          </div>
        </div>
        <div
          v-for="(m, i) in messages"
          :key="i"
          class="msg"
          :class="m.role === 'user' ? 'user' : 'ai'"
        >
          <div class="bubble" v-html="renderMarkdown(m.content)" />
        </div>
        <div v-if="sending" class="msg ai">
          <div class="bubble typing"><span class="dot" /><span class="dot" /><span class="dot" /></div>
        </div>
      </div>

      <footer class="ai-foot">
        <el-input
          v-model="input"
          size="small"
          placeholder="描述你的需求，如：找出今天失败的任务..."
          @keyup.enter="send(input)"
        >
          <template #append>
            <el-button :disabled="sending" @click="send(input)">发送</el-button>
          </template>
        </el-input>
      </footer>

      <!-- config dialog -->
      <el-dialog v-model="configVisible" title="模型配置" width="420px" append-to-body>
        <el-form :model="config" label-width="90px" size="small">
          <el-form-item label="Provider">
            <el-select v-model="config.provider" style="width: 100%">
              <el-option v-for="p in ['openai', 'azure', 'local', 'ollama']" :key="p" :label="p" :value="p" />
            </el-select>
          </el-form-item>
          <el-form-item label="Base URL">
            <el-input v-model="config.baseUrl" />
          </el-form-item>
          <el-form-item label="Model">
            <el-input v-model="config.model" />
          </el-form-item>
          <el-form-item label="API Key">
            <el-input v-model="config.apiKey" type="password" show-password :placeholder="config.apiKeySet ? '已配置，留空保持不变' : ''" />
          </el-form-item>
          <el-form-item label="Temperature">
            <el-slider v-model="temperature" :min="0" :max="1" :step="0.1" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button size="small" @click="testConfig">测试连接</el-button>
          <el-button size="small" @click="configVisible = false">取消</el-button>
          <el-button size="small" type="primary" @click="saveConfig">保存</el-button>
        </template>
      </el-dialog>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick, Close, Setting, Clock, Delete } from '@element-plus/icons-vue'
import { useAssistantStore } from '@/stores/assistant'
import { agentApi, type AgentSession } from '@/api/agent'

const assistant = useAssistantStore()

interface LocalMsg {
  role: 'user' | 'assistant'
  content: string
}

const input = ref('')
const sending = ref(false)
const messages = ref<LocalMsg[]>([])
const sessionId = ref<string | undefined>(undefined)
const sessions = ref<AgentSession[]>([])
const bodyRef = ref<HTMLElement>()

const configVisible = ref(false)
const temperature = ref(0.7)
const config = ref({
  provider: 'openai',
  baseUrl: '',
  model: '',
  apiKey: '',
  apiKeySet: false,
})

const quickQuestions = [
  '帮我分析今天失败的数据任务',
  '检查数据质量并给出建议',
  '找出包含敏感字段的表',
  '创建一个每天 8 点执行的调度任务',
]

function renderMarkdown(text: string): string {
  if (!text) return ''
  const esc = (s: string) =>
    s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  // bullets / numbered lists
  let t = esc(text)
  t = t.replace(/^(\s*)(\d+)\.\s+/gm, (_m, sp, num) => `${sp}<div class="li"><b>${num}.</b> `)
  t = t.replace(/^(\s*)[-•]\s+/gm, '<div class="li">• ')
  t = t.replace(/\n/g, '<br/>')
  t = t.replace(/<div class="li"><b>\d+\.<\/b>\s+(.*?)<br\/>/g, '<div class="li"><b>&#xA7;</b> $1<br/>')
  return t
}

async function loadSessions() {
  try {
    sessions.value = (await agentApi.sessions()) || []
  } catch {
    /* ignore */
  }
}
async function loadConfig() {
  try {
    const c = await agentApi.getConfig()
    config.value = {
      provider: c.provider || 'openai',
      baseUrl: c.baseUrl || '',
      model: c.model || '',
      apiKey: '',
      apiKeySet: !!c.apiKeySet,
    }
    temperature.value = c.temperature ?? 0.7
  } catch {
    /* ignore */
  }
}

async function openSession(s: AgentSession) {
  sessionId.value = s.sessionId
  messages.value = []
  try {
    const hist = (await agentApi.history(s.sessionId)) || []
    for (const h of hist) {
      if (h.action === 'user') messages.value.push({ role: 'user', content: h.message || '' })
      else if (h.action === 'assistant') messages.value.push({ role: 'assistant', content: h.reply || '' })
    }
  } catch {
    /* ignore */
  }
  scrollToBottom()
}

async function send(raw?: string) {
  const text = (raw ?? '').trim()
  if (!text || sending.value) return
  messages.value.push({ role: 'user', content: text })
  input.value = ''
  scrollToBottom()
  sending.value = true
  try {
    const res = await agentApi.chat({ sessionId: sessionId.value, message: text })
    if (res.sessionId) {
      sessionId.value = res.sessionId
      await loadSessions()
    }
    messages.value.push({ role: 'assistant', content: res.reply || '（无回复）' })
  } catch {
    messages.value.push({ role: 'assistant', content: '抱歉，处理时出现错误，请稍后重试。' })
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

function newSession() {
  sessionId.value = undefined
  messages.value = []
}

function scrollToBottom() {
  nextTick(() => {
    if (bodyRef.value) bodyRef.value.scrollTop = bodyRef.value.scrollHeight
  })
}

function onSessionCommand(s: AgentSession) {
  openSession(s)
}
async function deleteSession(s: AgentSession) {
  await agentApi.deleteSession(s.sessionId)
  await loadSessions()
  if (sessionId.value === s.sessionId) newSession()
}
function onConfigCommand(cmd: string) {
  if (cmd === 'new') newSession()
  else if (cmd === 'config') {
    configVisible.value = true
  }
}
async function saveConfig() {
  await agentApi.saveConfig({
    provider: config.value.provider,
    baseUrl: config.value.baseUrl,
    model: config.value.model,
    temperature: temperature.value,
    apiKey: config.value.apiKey,
  })
  ElMessage.success('配置已保存')
  configVisible.value = false
  config.value.apiKey = ''
}
async function testConfig() {
  const res = await agentApi.testConfig({
    provider: config.value.provider,
    model: config.value.model,
    baseUrl: config.value.baseUrl,
  })
  ElMessage.success((res.message as string) || '连接成功')
}

onMounted(() => {
  loadSessions()
  loadConfig()
})
</script>

<style scoped>
.ai-drawer-mask {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(15, 23, 42, 0.32);
  display: flex;
  justify-content: flex-end;
}
.ai-drawer {
  width: 380px;
  height: 100%;
  background: var(--td-surface);
  display: flex;
  flex-direction: column;
  box-shadow: -8px 0 30px rgba(15, 23, 42, 0.12);
}
.ai-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid var(--td-border);
}
.ai-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: var(--td-primary);
  font-size: 14px;
}
.ai-actions {
  display: flex;
  gap: 4px;
}
.sess-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  border-radius: 6px;
  color: var(--td-text-3);
  cursor: pointer;
}
.sess-btn:hover {
  background: var(--td-border-light);
}
.sess-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.sess-title {
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sess-del {
  color: var(--td-danger);
}
.ai-body {
  flex: 1;
  overflow: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.ai-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 32px 12px;
  color: var(--td-text-2);
}
.empty-icon {
  font-size: 36px;
  margin-bottom: 8px;
}
.empty-sub {
  font-size: 12px;
  color: var(--td-text-3);
  margin-top: 4px;
}
.quick-qs {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 18px;
  width: 100%;
}
.quick {
  border: 1px solid var(--td-border);
  background: var(--td-surface);
  color: var(--td-text-2);
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 12px;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.15s, color 0.15s;
}
.quick:hover {
  border-color: var(--td-primary);
  color: var(--td-primary);
}
.msg {
  display: flex;
}
.msg.user {
  justify-content: flex-end;
}
.msg.ai {
  justify-content: flex-start;
}
.bubble {
  max-width: 82%;
  padding: 10px 12px;
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.6;
}
.msg.user .bubble {
  background: var(--td-primary);
  color: #fff;
  border-top-right-radius: 3px;
}
.msg.ai .bubble {
  background: var(--td-bg);
  color: var(--td-text-1);
  border-top-left-radius: 3px;
}
.bubble :deep(.li) {
  margin: 2px 0;
}
.typing {
  display: flex;
  gap: 4px;
  align-items: center;
  padding: 12px;
}
.typing .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--td-text-4);
  animation: blink 1.2s infinite;
}
.typing .dot:nth-child(2) {
  animation-delay: 0.2s;
}
.typing .dot:nth-child(3) {
  animation-delay: 0.4s;
}
@keyframes blink {
  0%, 80%, 100% { opacity: 0.3; }
  40% { opacity: 1; }
}
.ai-foot {
  padding: 12px 16px;
  border-top: 1px solid var(--td-border);
}
</style>
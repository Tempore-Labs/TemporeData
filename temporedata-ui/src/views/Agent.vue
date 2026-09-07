<template>
  <div class="agent-page">
    <!-- 会话侧栏 -->
    <aside class="session-sidebar">
      <!-- 品牌头部 -->
      <div class="sidebar-brand">
        <div class="brand-badge">
          <el-icon :size="16"><ChatDotRound /></el-icon>
        </div>
        <div class="brand-text">
          <span class="brand-title">Agents</span>
          <span class="brand-sub">TemporeData AI 助手</span>
        </div>
      </div>

      <!-- 新对话 -->
      <button class="new-chat-btn" @click="newChat">
        <el-icon><Plus /></el-icon>
        <span>新建对话</span>
        <kbd>F</kbd>
      </button>

      <!-- 会话搜索 -->
      <div class="session-search">
        <el-input
          v-model="sessionQuery"
          :prefix-icon="Search"
          placeholder="搜索会话..."
          clearable
          size="small"
        />
      </div>

      <!-- 会话列表 -->
      <div class="session-list" v-loading="sessionsLoading">
        <div
          v-for="s in filteredSessions"
          :key="s.sessionId"
          :class="['session-item', { active: s.sessionId === currentSessionId }]"
          @click="selectSession(s)"
        >
          <el-icon class="session-icon" :class="{ active: s.sessionId === currentSessionId }">
            <ChatLineRound />
          </el-icon>
          <div class="session-body">
            <div class="session-title">{{ s.title }}</div>
            <div class="session-meta">{{ s.messageCount }} 条消息</div>
          </div>
          <el-tooltip content="删除会话" placement="top">
            <el-icon class="session-del" @click.stop="removeSession(s)"><Delete /></el-icon>
          </el-tooltip>
        </div>
        <div v-if="!filteredSessions.length && !sessionsLoading" class="session-empty">
          <el-icon :size="26"><ChatLineRound /></el-icon>
          <span>{{ sessionQuery ? '未找到匹配的会话' : '暂无历史对话' }}</span>
        </div>
      </div>

      <!-- 底部：模型设置 -->
      <button
        :class="['sidebar-config-btn', { active: panel === 'config' }]"
        @click="toggleConfigPanel"
      >
        <el-icon><Setting /></el-icon>
        <span class="config-label">模型设置</span>
        <span
          v-if="config && config.enabled"
          class="config-dot"
          title="已启用大模型"
        ></span>
        <span class="model-badge" :class="{ warn: !(config && config.enabled) }">
          {{ config && config.enabled ? currentModel || '已配置' : '内置规则' }}
        </span>
      </button>
    </aside>

    <!-- 主画布 -->
    <main class="canvas">
      <!-- 视图1：聊天 -->
      <template v-if="panel === 'chat'">
        <header class="canvas-header">
          <div class="header-left">
            <span class="canvas-title">{{ currentSessionId ? currentTitle : '新对话' }}</span>
            <span class="header-status" v-if="config && config.enabled">
              <span class="status-badge live"></span>
              大模型在线
            </span>
            <span class="header-status" v-else>
              <span class="status-badge"></span>
              内置规则模式
            </span>
          </div>
          <div class="header-actions">
            <el-tag v-if="config && config.enabled" size="small" type="primary" effect="light" round>
              <span class="model-name">{{ currentModel }}</span>
            </el-tag>
            <el-tooltip content="新建对话" placement="bottom">
              <el-button text :icon="EditPen" circle size="small" @click="newChat" />
            </el-tooltip>
            <el-tooltip content="模型设置" placement="bottom">
              <el-button
                text
                :icon="Setting"
                circle
                size="small"
                :class="['header-config-btn', { active: panel === 'config' }]"
                @click="toggleConfigPanel"
              />
            </el-tooltip>
          </div>
        </header>

        <div class="canvas-messages" ref="messagesRef">
          <!-- 欢迎页 -->
          <div v-if="messages.length === 0" class="welcome">
            <div class="welcome-hero">
              <div class="welcome-logo">
                <el-icon :size="42"><ChatDotRound /></el-icon>
              </div>
              <h1 class="welcome-title">有什么可以帮您？</h1>
              <p class="welcome-sub">
                {{ config && config.enabled
                  ? `已连接 ${currentModel} 模型，可直接输入问题开始对话`
                  : '当前为内置规则应答。前往右侧「模型设置」连接大模型以获得完整能力。' }}
              </p>
            </div>

            <div class="capability-grid">
              <button v-for="c in capabilities" :key="c.title" class="capability-card" @click="sendSuggest(c.example)">
                <div class="capability-icon" :style="{ background: c.bg, color: c.color }">
                  <el-icon :size="18"><component :is="c.icon" /></el-icon>
                </div>
                <div class="capability-info">
                  <span class="capability-title">{{ c.title }}</span>
                  <span class="capability-desc">{{ c.desc }}</span>
                </div>
                <el-icon class="capability-arrow"><ArrowRight /></el-icon>
              </button>
            </div>

            <div class="welcome-foot">选择一种能力或直接输入您的问题</div>
          </div>

          <!-- 消息 -->
          <template v-for="(msg, idx) in messages" :key="idx">
            <div
              :class="['message-row', msg.role === 'assistant' ? 'msg-left' : 'msg-right']"
            >
              <div class="msg-avatar" v-if="msg.role === 'assistant'">
                <div class="ai-avatar"><el-icon :size="17"><ChatDotRound /></el-icon></div>
              </div>
              <div :class="['msg-bubble', msg.role === 'assistant' ? 'bubble-ai' : 'bubble-user']">
                <div class="msg-content" v-if="msg.role === 'assistant'" v-html="renderMarkdown(msg.content)"></div>
                <div class="msg-content" v-else>{{ msg.content }}</div>
                <div class="msg-actions" v-if="msg.role === 'assistant'">
                  <button class="msg-action" @click="copyText(msg.content)">
                    <el-icon :size="12"><CopyDocument /></el-icon>
                    复制
                  </button>
                </div>
              </div>
              <div class="msg-avatar" v-if="msg.role === 'user'">
                <div class="user-avatar"><el-icon :size="17"><User /></el-icon></div>
              </div>
            </div>
          </template>

          <!-- 思考中 -->
          <div v-if="sending" class="message-row msg-left">
            <div class="msg-avatar">
              <div class="ai-avatar"><el-icon :size="17"><ChatDotRound /></el-icon></div>
            </div>
            <div class="msg-bubble bubble-ai typing-bubble">
              <span class="typing-dot"></span>
              <span class="typing-dot"></span>
              <span class="typing-dot"></span>
              <span class="thinking-label">正在思考</span>
            </div>
          </div>
        </div>

        <!-- 输入区 -->
        <div class="canvas-input">
          <div class="input-toolbar" v-if="config && config.enabled">
            <span class="input-model-tag">
              <el-icon :size="13"><MagicStick /></el-icon>
              {{ currentModel }}
            </span>
          </div>
          <div class="input-box">
            <textarea
              ref="inputRef"
              v-model="inputText"
              rows="1"
              :placeholder="sending ? '正在思考中...' : '输入您的问题，Enter 发送，Shift + Enter 换行'"
              :disabled="sending"
              @keydown.enter.exact.prevent="handleSend"
              @input="autoResize"
            ></textarea>
            <div class="input-actions">
              <span class="word-count" v-show="inputText.trim()">{{ inputText.trim().length }}</span>
              <button class="send-btn" :disabled="!inputText.trim() || sending" @click="handleSend">
                <el-icon v-if="!sending"><Promotion /></el-icon>
                <span v-else class="stop-dots">■</span>
              </button>
            </div>
          </div>
          <div class="input-hint">
            <el-icon :size="12"><InfoFilled /></el-icon>
            AI 生成内容仅供参考，请注意甄别
          </div>
        </div>
      </template>

      <!-- 视图2：模型配置 -->
      <template v-else>
        <header class="canvas-header">
          <div class="header-left">
            <span class="canvas-title">模型设置</span>
            <span class="header-status">
              <span class="status-badge" :class="{ live: config && config.enabled }"></span>
              {{ config && config.enabled ? '已启用' : '未启用' }}
            </span>
          </div>
          <div class="header-actions">
            <el-button text @click="handleCancelConfig">返回对话</el-button>
          </div>
        </header>

        <div class="config-panel">
          <div class="config-card">
            <div class="config-card-header">
              <div class="config-card-icon"><el-icon><Cpu /></el-icon></div>
              <div>
                <h3 class="config-card-title">模型接口配置</h3>
                <p class="config-card-sub">配置 OpenAI 兼容的大模型接口，用于 AI 助手的智能应答</p>
              </div>
            </div>

            <el-form label-width="110px" label-position="right" class="config-form">
              <el-form-item label="供应商">
                <el-select v-model="configForm.provider" style="width: 100%" @change="onProviderChange">
                  <el-option label="自定义" value="CUSTOM" />
                  <el-option label="OpenAI" value="OPENAI" />
                  <el-option label="DeepSeek（深度求索）" value="DEEPSEEK" />
                  <el-option label="通义千问（阿里云）" value="QWEN" />
                  <el-option label="Kimi（月之暗面）" value="MOONSHOT" />
                </el-select>
              </el-form-item>
              <el-form-item label="Base URL">
                <el-input v-model="configForm.baseUrl" placeholder="https://api.openai.com/v1" />
                <div class="form-hint">OpenAI 兼容接口地址，末尾无需 /chat/completions</div>
              </el-form-item>
              <el-form-item label="API Key">
                <el-input v-model="configForm.apiKey" type="password" show-password :placeholder="apiKeyPlaceholder" />
                <div class="form-hint">留空则保留原有 Key（已有配置时）</div>
              </el-form-item>
              <el-form-item label="模型名称">
                <el-input v-model="configForm.model" placeholder="gpt-4o-mini / deepseek-chat / qwen-plus" />
              </el-form-item>
              <el-form-item label="温度">
                <el-slider v-model="configForm.temperature" :min="0" :max="2" :step="0.1" show-input />
                <div class="form-hint">较低温度更适合稳定/确定性输出，较高温度更具创造性</div>
              </el-form-item>
              <el-form-item label="启用模型">
                <el-switch v-model="configForm.enabled" active-text="对话使用大模型应答" />
              </el-form-item>
            </el-form>

            <div class="config-actions">
              <el-button :loading="testing" :icon="Connection" @click="handleTest">测试连接</el-button>
              <el-button @click="handleCancelConfig">取消</el-button>
              <el-button type="primary" :loading="savingConfig" :icon="Check" @click="handleSaveConfig">
                保存配置
              </el-button>
            </div>
          </div>
        </div>
      </template>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, ChatDotRound, ChatLineRound, Delete, Setting, User, Promotion,
  CopyDocument, Search, ArrowRight, EditPen, MagicStick, InfoFilled,
  Cpu, Connection, Check, TrendCharts, DataAnalysis, DocumentChecked
} from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import { agentApi } from '@/api/modules/agent'

const md = new MarkdownIt({ html: false, linkify: true, breaks: true })

const PROVIDER_TEMPLATES = {
  OPENAI: { baseUrl: 'https://api.openai.com/v1', model: 'gpt-4o-mini' },
  DEEPSEEK: { baseUrl: 'https://api.deepseek.com/v1', model: 'deepseek-chat' },
  QWEN: { baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1', model: 'qwen-plus' },
  MOONSHOT: { baseUrl: 'https://api.moonshot.cn/v1', model: 'moonshot-v1-8k' }
}

// 能力卡片
const capabilities = [
  {
    title: '数据查询',
    desc: '生成检索 / 聚合 SQL',
    example: '帮我生成一个查询报表的 SQL',
    icon: DataAnalysis,
    bg: '#e8f1fe',
    color: '#1a6ff5'
  },
  {
    title: '数据处理',
    desc: '构建数据处理流程',
    example: '推荐一个数据处理流程',
    icon: TrendCharts,
    bg: '#e7f6ef',
    color: '#10b981'
  },
  {
    title: '质量检查',
    desc: '评估数据质量状况',
    example: '如何检查数据质量？',
    icon: DocumentChecked,
    bg: '#fef3e2',
    color: '#f59e0b'
  }
]

// ---- 会话 ----
const sessions = ref([])
const sessionsLoading = ref(false)
const sessionQuery = ref('')
const currentSessionId = ref(null)
const currentTitle = ref('')
const messages = reactive([])
const sending = ref(false)
const inputText = ref('')
const messagesRef = ref(null)
const inputRef = ref(null)
const panel = ref('chat')

// ---- 配置 ----
const config = ref(null)
const currentModel = ref('')
const configForm = reactive({ provider: 'CUSTOM', baseUrl: '', apiKey: '', model: '', temperature: 0.7, enabled: true })
const apiKeyPlaceholder = ref('')
const savingConfig = ref(false)
const testing = ref(false)

const filteredSessions = computed(() => {
  const kw = sessionQuery.value.trim().toLowerCase()
  if (!kw) return sessions.value
  return sessions.value.filter(s => (s.title || '').toLowerCase().includes(kw))
})

function renderMarkdown(text) {
  return md.render(text || '')
}

function scrollToBottom() {
  nextTick(() => {
    const el = messagesRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function autoResize(e) {
  const t = e.target
  t.style.height = 'auto'
  t.style.height = Math.min(t.scrollHeight, 220) + 'px'
}

function focusInput() {
  nextTick(() => inputRef.value?.focus())
}

// ---- 快捷新建对话 (F key) ----
function onKeydown(e) {
  if (panel.value !== 'chat') return
  const target = e.target
  const isTyping = target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable)
  if (e.key === 'f' && !e.ctrlKey && !e.metaKey && !e.altKey && !isTyping) {
    e.preventDefault()
    newChat()
  }
}

// ---- 会话 ----
async function loadSessions() {
  sessionsLoading.value = true
  try {
    sessions.value = await agentApi.sessions()
  } catch (e) {
    ElMessage.error(e.message || '加载会话失败')
  } finally {
    sessionsLoading.value = false
  }
}

function newChat() {
  panel.value = 'chat'
  currentSessionId.value = null
  currentTitle.value = ''
  messages.splice(0)
  nextTick(() => {
    inputText.value = ''
    focusInput()
  })
}

async function selectSession(s) {
  panel.value = 'chat'
  currentSessionId.value = s.sessionId
  currentTitle.value = s.title
  messages.splice(0)
  try {
    const history = await agentApi.history(s.sessionId)
    history.forEach(h => messages.push({ role: h.role, content: h.content }))
    scrollToBottom()
  } catch (e) {
    ElMessage.error(e.message || '加载历史失败')
  }
}

async function removeSession(s) {
  try {
    await ElMessageBox.confirm(`删除对话「${s.title}」？`, '删除会话', { type: 'warning' })
  } catch (_) {
    return
  }
  try {
    await agentApi.deleteSession(s.sessionId)
    if (currentSessionId.value === s.sessionId) newChat()
    await loadSessions()
    ElMessage.success('会话已删除')
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

// ---- 发送 ----
async function handleSend() {
  const text = inputText.value.trim()
  if (!text || sending.value) return

  messages.push({ role: 'user', content: text })
  inputText.value = ''
  sending.value = true
  scrollToBottom()

  try {
    const res = await agentApi.chat({
      message: text,
      sessionId: currentSessionId.value || undefined,
      modelName: (config.value && config.value.enabled && currentModel.value) || undefined
    })
    messages.push({ role: 'assistant', content: res.reply })
    if (res.sessionId && res.sessionId !== currentSessionId.value) {
      currentSessionId.value = res.sessionId
      currentTitle.value = text.length > 24 ? text.slice(0, 24) + '…' : text
    }
    await loadSessions()
  } catch (e) {
    messages.push({ role: 'assistant', content: '抱歉，请求失败：' + (e.message || '网络错误') + '。请检查大模型配置。' })
  } finally {
    sending.value = false
    scrollToBottom()
    focusInput()
  }
}

function sendSuggest(q) {
  inputText.value = q
  handleSend()
}

async function copyText(text) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制')
  } catch (_) {
    ElMessage.error('复制失败')
  }
}

// ---- 配置 ----
async function loadConfig() {
  try {
    config.value = await agentApi.getConfig()
    if (config.value && config.value.enabled) {
      currentModel.value = config.value.model || ''
    }
  } catch (_) { /* ignore */ }
}

function handleCancelConfig() {
  panel.value = 'chat'
}

function toggleConfigPanel() {
  if (panel.value === 'config') {
    handleCancelConfig()
  } else {
    openConfigPanel()
  }
}

function openConfigPanel() {
  panel.value = 'config'
  configForm.provider = config.value?.provider || 'CUSTOM'
  configForm.baseUrl = config.value?.baseUrl || ''
  configForm.apiKey = ''
  configForm.model = config.value?.model || ''
  configForm.temperature = config.value?.temperature ?? 0.7
  configForm.enabled = config.value?.enabled ?? true
  apiKeyPlaceholder.value = config.value?.apiKeySet ? '已配置（' + (config.value.apiKeyMasked || '****') + '），留空保持不变' : '请输入 API Key'
}

function onProviderChange() {
  const tpl = PROVIDER_TEMPLATES[configForm.provider]
  if (!tpl) return
  const prevTpl = PROVIDER_TEMPLATES[config.value?.provider]
  if (!configForm.baseUrl || (prevTpl && configForm.baseUrl === prevTpl.baseUrl)) {
    configForm.baseUrl = tpl.baseUrl
    configForm.model = tpl.model
  }
}

async function handleTest() {
  if (!configForm.baseUrl || (!configForm.apiKey && !config.value?.apiKeySet)) {
    ElMessage.warning('请先填写 Base URL 和 API Key')
    return
  }
  testing.value = true
  try {
    const res = await agentApi.testConfig({
      provider: configForm.provider,
      baseUrl: configForm.baseUrl,
      apiKey: configForm.apiKey || '****' + (config.value?.apiKeyMasked || ''),
      model: configForm.model,
      temperature: configForm.temperature
    })
    if (res.ok) {
      ElMessage.success(res.message)
    } else {
      ElMessage.error(res.message)
    }
  } catch (e) {
    ElMessage.error(e.message || '测试失败')
  } finally {
    testing.value = false
  }
}

async function handleSaveConfig() {
  if (!configForm.baseUrl || !configForm.model) {
    ElMessage.warning('请填写 Base URL 和模型名称')
    return
  }
  savingConfig.value = true
  try {
    config.value = await agentApi.saveConfig({
      provider: configForm.provider,
      baseUrl: configForm.baseUrl,
      apiKey: configForm.apiKey,
      model: configForm.model,
      temperature: configForm.temperature,
      enabled: configForm.enabled
    })
    if (config.value.enabled) currentModel.value = config.value.model || ''
    panel.value = 'chat'
    ElMessage.success('配置已保存')
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    savingConfig.value = false
  }
}

onMounted(() => {
  loadSessions()
  loadConfig()
  document.addEventListener('keydown', onKeydown)
})
</script>

<style scoped>
/* ========== 整体容器 ========== */
.agent-page {
  display: flex;
  height: calc(100vh - 148px);
  min-height: 520px;
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid var(--spark-border, #e8eaed);
}

/* ---- 会话侧栏 ---- */
.session-sidebar {
  width: 264px;
  flex-shrink: 0;
  background: #fafbfd;
  border-right: 1px solid #e8eaed;
  display: flex;
  flex-direction: column;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 16px 12px;
}

.brand-badge {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #1a6ff5, #4f8bf8);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 10px rgba(26, 111, 245, 0.25);
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.brand-title {
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
}

.brand-sub {
  font-size: 11px;
  color: #9ca3af;
}

.new-chat-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 16px 12px;
  padding: 9px 12px;
  border: 1px solid #1a6ff5;
  background: #1a6ff5;
  color: #fff;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.new-chat-btn:hover {
  background: #1558c4;
  border-color: #1558c4;
}

.new-chat-btn kbd {
  margin-left: auto;
  padding: 1px 6px;
  font-size: 11px;
  font-family: inherit;
  background: rgba(255, 255, 255, 0.18);
  border-radius: 4px;
}

.session-search {
  padding: 0 16px 10px;
}

.session-search :deep(.el-input__wrapper) {
  background: #fff;
  border-radius: 7px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 10px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.session-list::-webkit-scrollbar {
  width: 4px;
}

.session-list::-webkit-scrollbar-thumb {
  background: #e0e4ea;
  border-radius: 4px;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}

.session-item:hover {
  background: #f0f2f5;
}

.session-item.active {
  background: #e8f1fe;
}

.session-icon {
  color: #9ca3af;
  flex-shrink: 0;
  font-size: 17px;
}

.session-icon.active {
  color: #1a6ff5;
}

.session-body {
  flex: 1;
  min-width: 0;
}

.session-title {
  font-size: 13px;
  color: #374151;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-item.active .session-title {
  color: #1a6ff5;
  font-weight: 600;
}

.session-meta {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 2px;
}

.session-del {
  color: #9ca3af;
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.15s;
  cursor: pointer;
}

.session-item:hover .session-del {
  opacity: 1;
}

.session-del:hover {
  color: #ef4444;
}

.session-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #9ca3af;
  font-size: 13px;
  padding: 40px 0;
}

.sidebar-config-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 8px 12px 12px;
  padding: 10px 12px;
  border: none;
  border-radius: 8px;
  background: #fff;
  color: #4b5563;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.sidebar-config-btn:hover {
  background: #f0f2f5;
}

.sidebar-config-btn.active {
  background: #e8f1fe;
  color: #1a6ff5;
}

.config-label {
  flex-shrink: 0;
}

.config-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.15);
  flex-shrink: 0;
}

.model-badge {
  margin-left: auto;
  font-size: 11px;
  font-weight: 500;
  background: #e7f6ef;
  color: #10b981;
  padding: 2px 8px;
  border-radius: 10px;
  max-width: 92px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-badge.warn {
  background: #f0f2f5;
  color: #9ca3af;
}

/* ---- 主画布 ---- */
.canvas {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: #fff;
}

.canvas-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  border-bottom: 1px solid #e8eaed;
  background: #fff;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.canvas-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 320px;
}

.header-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #9ca3af;
  white-space: nowrap;
}

.status-badge {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #d1d5db;
  flex-shrink: 0;
}

.status-badge.live {
  background: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.15);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.model-name {
  font-family: var(--spark-font-mono, monospace);
  font-size: 12px;
}

.header-config-btn {
  color: #6b7280;
}

.header-config-btn.active {
  color: #1a6ff5;
  background: #e8f1fe;
}

/* ---- 聊天区 ---- */
.canvas-messages {
  flex: 1;
  overflow-y: auto;
  padding: 28px 24px;
  background: linear-gradient(180deg, #fafbfd 0%, #fff 40%);
}

.canvas-messages::-webkit-scrollbar {
  width: 6px;
}

.canvas-messages::-webkit-scrollbar-thumb {
  background: #e0e4ea;
  border-radius: 3px;
}

/* 欢迎页 */
.welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 36px 20px 20px;
}

.welcome-hero {
  text-align: center;
  margin-bottom: 30px;
}

.welcome-logo {
  width: 80px;
  height: 80px;
  margin: 0 auto 18px;
  border-radius: 22px;
  background: linear-gradient(135deg, #1a6ff5, #7c3aed);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12px 30px rgba(26, 111, 245, 0.28);
}

.welcome-title {
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 8px;
}

.welcome-sub {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.capability-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  width: 100%;
  max-width: 760px;
}

.capability-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 14px;
  border: 1px solid #e8eaed;
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  text-align: left;
  transition: all 0.2s;
}

.capability-card:hover {
  border-color: #1a6ff5;
  box-shadow: 0 6px 18px rgba(26, 111, 245, 0.1);
  transform: translateY(-2px);
}

.capability-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.capability-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.capability-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.capability-desc {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.capability-arrow {
  margin-left: auto;
  color: #c0c4cc;
  flex-shrink: 0;
}

.capability-card:hover .capability-arrow {
  color: #1a6ff5;
}

.welcome-foot {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 26px;
}

/* 消息 */
.message-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 22px;
}

.msg-left {
  justify-content: flex-start;
}

.msg-right {
  justify-content: flex-end;
}

.msg-avatar {
  flex-shrink: 0;
}

.ai-avatar {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  background: linear-gradient(135deg, #1a6ff5, #4f8bf8);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 3px 8px rgba(26, 111, 245, 0.2);
}

.user-avatar {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  background: #eef0f3;
  color: #6b7280;
  display: flex;
  align-items: center;
  justify-content: center;
}

.msg-bubble {
  max-width: 78%;
  position: relative;
  font-size: 14px;
  line-height: 1.65;
  word-break: break-word;
}

.bubble-ai {
  background: #f7f8fa;
  color: #1f2937;
  padding: 12px 16px;
  border-radius: 4px 14px 14px 14px;
  border: 1px solid #eef0f3;
}

.bubble-user {
  background: #1a6ff5;
  color: #fff;
  padding: 10px 16px;
  border-radius: 14px 4px 14px 14px;
  box-shadow: 0 3px 10px rgba(26, 111, 245, 0.18);
}

.msg-content {
  white-space: pre-wrap;
}

.msg-actions {
  display: flex;
  gap: 6px;
  margin-top: 8px;
  opacity: 0;
  transition: opacity 0.15s;
}

.bubble-ai:hover .msg-actions {
  opacity: 1;
}

.msg-action {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: transparent;
  color: #9ca3af;
  font-size: 12px;
  cursor: pointer;
  padding: 3px 6px;
  border-radius: 6px;
  transition: all 0.15s;
}

.msg-action:hover {
  color: #1a6ff5;
  background: #e8f1fe;
}

/* Markdown 渲染 */
.msg-content :deep(p) {
  margin: 0 0 8px;
}

.msg-content :deep(p:last-child) {
  margin-bottom: 0;
}

.msg-content :deep(pre) {
  background: #0f172a;
  color: #e2e8f0;
  padding: 12px 14px;
  border-radius: 8px;
  overflow-x: auto;
  font-size: 13px;
  line-height: 1.55;
  margin: 8px 0;
  border: 1px solid #1e293b;
}

.msg-content :deep(code) {
  font-family: var(--spark-font-mono, 'SF Mono', 'Menlo', 'Consolas', monospace);
}

.msg-content :deep(:not(pre) > code) {
  background: #e8f1fe;
  color: #1a6ff5;
  padding: 1px 5px;
  border-radius: 4px;
  font-size: 13px;
}

.msg-content :deep(ul),
.msg-content :deep(ol) {
  margin: 6px 0;
  padding-left: 20px;
}

.msg-content :deep(a) {
  color: #1a6ff5;
}

.msg-content :deep(blockquote) {
  margin: 8px 0;
  padding: 4px 12px;
  border-left: 3px solid #1a6ff5;
  color: #6b7280;
  background: #f4f8ff;
  border-radius: 0 6px 6px 0;
}

.msg-content :deep(table) {
  border-collapse: collapse;
  margin: 8px 0;
  width: 100%;
}

.msg-content :deep(th),
.msg-content :deep(td) {
  border: 1px solid #e5e9f0;
  padding: 6px 10px;
  font-size: 13px;
}

.msg-content :deep(th) {
  background: #f5f7fa;
  font-weight: 600;
}

.msg-content :deep(h1),
.msg-content :deep(h2),
.msg-content :deep(h3) {
  margin: 12px 0 8px;
  color: #111827;
}

.msg-content :deep(h1) {
  font-size: 18px;
}

.msg-content :deep(h2) {
  font-size: 16px;
}

.msg-content :deep(h3) {
  font-size: 14px;
}

/* 思考中 */
.typing-bubble {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 14px 18px;
}

.typing-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #9ca3af;
  animation: typing-bounce 1.4s infinite ease-in-out both;
}

.typing-dot:nth-child(1) { animation-delay: -0.32s; }
.typing-dot:nth-child(2) { animation-delay: -0.16s; }
.typing-dot:nth-child(3) { animation-delay: 0s; }

.thinking-label {
  font-size: 12px;
  color: #9ca3af;
  margin-left: 6px;
}

@keyframes typing-bounce {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.4;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

/* ---- 输入区 ---- */
.canvas-input {
  padding: 10px 20px 10px;
  border-top: 1px solid #e8eaed;
  background: #fff;
}

.input-toolbar {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
  padding-left: 4px;
}

.input-model-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #1a6ff5;
  background: #f4f8ff;
  border: 1px solid #d6e4ff;
  padding: 2px 10px;
  border-radius: 20px;
}

.input-box {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  border: 1px solid #d9dde3;
  border-radius: 12px;
  padding: 10px 10px 10px 14px;
  background: #fff;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.input-box:focus-within {
  border-color: #1a6ff5;
  box-shadow: 0 0 0 3px rgba(26, 111, 245, 0.1);
}

.input-box textarea {
  flex: 1;
  border: none;
  outline: none;
  resize: none;
  font-size: 14px;
  line-height: 1.6;
  font-family: inherit;
  color: #1f2937;
  max-height: 220px;
  background: transparent;
}

.input-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.word-count {
  font-size: 11px;
  color: #c0c4cc;
  font-variant-numeric: tabular-nums;
}

.send-btn {
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  border: none;
  border-radius: 10px;
  background: #1a6ff5;
  color: #fff;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.send-btn:hover:not(:disabled) {
  background: #1558c4;
}

.send-btn:disabled {
  background: #d6dbe2;
  cursor: not-allowed;
}

.stop-dots {
  font-size: 13px;
  letter-spacing: 0;
}

.input-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-size: 11px;
  color: #c0c4cc;
  margin-top: 8px;
}

/* ---- 配置面板 ---- */
.config-panel {
  flex: 1;
  overflow-y: auto;
  padding: 28px;
  background: #fafbfd;
}

.config-card {
  max-width: 760px;
  margin: 0 auto;
  background: #fff;
  border: 1px solid #e8eaed;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.config-card-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-bottom: 18px;
  border-bottom: 1px solid #eef0f3;
  margin-bottom: 22px;
}

.config-card-icon {
  width: 44px;
  height: 44px;
  border-radius: 11px;
  background: linear-gradient(135deg, #1a6ff5, #4f8bf8);
  color: #fff;
  font-size: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.config-card-title {
  font-size: 17px;
  font-weight: 700;
  color: #1f2937;
  margin: 0;
}

.config-card-sub {
  font-size: 13px;
  color: #9ca3af;
  margin: 3px 0 0;
}

.config-form {
  max-width: 620px;
}

.form-hint {
  font-size: 12px;
  color: #9ca3af;
  line-height: 1.5;
  margin-top: 4px;
}

.config-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #eef0f3;
}

/* 响应式 */
@media (max-width: 900px) {
  .capability-grid {
    grid-template-columns: 1fr;
  }
  .session-sidebar {
    width: 200px;
  }
  .bubble-ai,
  .bubble-user {
    max-width: 90%;
  }
}
</style>
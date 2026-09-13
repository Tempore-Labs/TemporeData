/** AI Assistant (sys.agent) API. */
import { apiGet, apiPost, apiDelete } from './http'

export interface AgentMessage {
  id?: string
  message?: string
  reply?: string
  context?: string
  sessionId?: string
  modelName?: string
  action?: 'user' | 'assistant'
  ok?: boolean
  createDateTime?: string
}

export interface AgentChatPayload {
  sessionId?: string
  message?: string
  context?: string
  modelName?: string
}

export interface AgentConfig {
  provider?: string
  baseUrl?: string
  apiKey?: string
  model?: string
  temperature?: number
  enabled?: boolean
  apiKeySet?: boolean
  apiKeyMasked?: string
}

export interface AgentSession {
  sessionId: string
  title?: string
  messageCount?: number
  lastTime?: string
}

export const agentApi = {
  chat: (payload: AgentChatPayload) => apiPost<AgentMessage>('/agent/chat', payload),
  getConfig: () => apiGet<AgentConfig>('/agent/config'),
  saveConfig: (cfg: AgentConfig) => apiPost<AgentConfig>('/agent/config', cfg),
  testConfig: (cfg: AgentConfig) =>
    apiPost<Record<string, unknown>>('/agent/config/test', cfg),
  sessions: () => apiGet<AgentSession[]>('/agent/sessions'),
  history: (sessionId: string) => apiGet<AgentMessage[]>(`/agent/history/${sessionId}`),
  deleteSession: (sessionId: string) => apiDelete<void>(`/agent/session/${sessionId}`),
}
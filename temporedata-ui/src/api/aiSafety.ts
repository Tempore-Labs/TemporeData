/** AI safety gate API (POST /api/ai-safety/check: JSON body + optional admins query param). */
import http from './http'
import type { BaseResponse } from '@/types/base'
import type { AiCommandRequest, AiPolicyDecision } from '@/types/aiSafety'

export const aiSafetyApi = {
  check: async (request: AiCommandRequest, admins?: string[]) => {
    const res = await http.post<BaseResponse<AiPolicyDecision>>('/ai-safety/check', request, {
      params: admins && admins.length ? { admins } : undefined,
    })
    return res.data.data
  },
}

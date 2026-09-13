/**
 * Enterprise gateway API: AppKey verify + token-bucket rate-limit probe.
 *
 * Uses raw fetch (not the shared axios instance) because the gateway legitimately
 * returns HTTP 401/429 as expected probe outcomes — the axios interceptor would
 * misinterpret a 401 as a stale session and force a logout.
 */
export interface GatewayProbe {
  http: number
  code: number
  msg: string
  remaining?: number
}

/** Attach the Bearer token header the same way the axios instance does. */
function authHeaders(): Record<string, string> {
  const token = localStorage.getItem('td_token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

async function probe(url: string): Promise<GatewayProbe> {
  const res = await fetch(url, { headers: { 'Content-Type': 'application/json', ...authHeaders() } })
  let body: any = {}
  try {
    body = await res.json()
  } catch {
    /* non-json */
  }
  return { http: res.status, code: body.code, msg: body.msg, remaining: body.data }
}

export const gatewayApi = {
  /** Validate an AppKey (expected 401 for rejected keys). */
  check: (appKey: string) => probe(`/api/gateway/check?appKey=${encodeURIComponent(appKey)}`),
  /** Rate-limit probe: consumes a token; 429 when burst exhausted. */
  ratelimit: (consumer: string, burst: number) =>
    probe(`/api/gateway/ratelimit?consumer=${encodeURIComponent(consumer)}&burst=${burst}`),
}
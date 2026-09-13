#!/usr/bin/env node
/**
 * Real-browser smoke test for the TemporeData Vue3 UI (temporedata-ui, vite :5173).
 *
 * Drives headless Chrome over the raw DevTools Protocol (no deps).
 * Asserts:
 *   1. Login form accepts admin/admin123 and redirects to /dashboard.
 *   2. AppShell (sidebar + topbar) mounts; sidebar nav has 12 items.
 *   3. Each page route mounts its root node and produces no console errors.
 *   4. Screenshot captured for the dashboard.
 *
 * Usage:
 *   node scripts/ui-smoke.cjs [--frontend http://localhost:5173] [--backend http://localhost:8080]
 *     [--username admin] [--password admin123] [--port 9224] [--out .]
 */
const fs = require('fs')
const path = require('path')
const { spawn } = require('child_process')

const ARGS = (() => {
  const a = process.argv.slice(2); const o = {}
  const k = ['frontend','backend','username','password','port','out','chrome']
  for (let i = 0; i < a.length; i++) {
    if (a[i].startsWith('--') && k.includes(a[i].slice(2)) && a[i+1] != null) o[a[i].slice(2)] = a[++i]
  }
  return o
})()
const FRONTEND = ARGS.frontend || 'http://localhost:5173'
const BACKEND = ARGS.backend || 'http://localhost:8080'
const USER = ARGS.username || 'admin'
const PASSWORD = ARGS.password || 'admin123'
const CDP_PORT = Number(ARGS.port || 9224)
const OUT = ARGS.out || '.'
function resolveChrome() {
  if (ARGS.chrome) return ARGS.chrome
  const cands = []
  if (process.env.CHROME_BIN) cands.push(process.env.CHROME_BIN)
  if (process.platform === 'darwin') cands.push('/Applications/Google Chrome.app/Contents/MacOS/Google Chrome')
  cands.push('/usr/bin/google-chrome', '/usr/bin/chromium')
  for (const c of cands) if (fs.existsSync(c)) return c
  return 'google-chrome'
}
const CHROME = resolveChrome()
const sleep = (ms) => new Promise(r => setTimeout(r, ms))
const results = []
function check(name, pass, detail = '') { results.push({ name, pass: !!pass, detail }); console.log(`UI_${pass ? 'OK' : 'FAIL'} ${name} ${detail || ''}`) }
function fail(name, detail = '') { check(name, false, detail) }
async function waitFor(fn, ms, step) { const t0 = Date.now(); while (Date.now() - t0 < ms) { try { if (await fn()) return true } catch {} await sleep(step || 300) } return false }

const PAGES = [
  ['/dashboard', '.dashboard'],
  ['/datasource', '.page'],
  ['/catalog', '.catalog'],
  ['/cluster', '.page'],
  ['/container', '.page'],
  ['/resource', '.page'],
  ['/lineage', '.lineage-page'],
  ['/sql-editor', '.sql-page'],
  ['/workflow', '.page'],
  ['/scheduler', '.page'],
  ['/monitor', '.page'],
  ['/alarm', '.page'],
  ['/audit', '.page'],
  ['/sensitive', '.page'],
  ['/security', '.page'],
  ['/tag', '.page'],
  ['/data-service', '.page'],
  ['/report', '.page'],
  ['/blacklist', '.page'],
  ['/ha', '.page'],
  ['/gateway', '.page'],
  ['/perm', '.page'],
  ['/quality', '.page'],
  ['/datasets', '.page'],
  ['/asset', '.page'],
  ['/contract', '.page'],
  ['/cost', '.page'],
  ['/incidents', '.page'],
  ['/ai-safety', '.page'],
  ['/rca', '.page'],
  ['/system', '.el-tabs'],
  ['/ingestion', '.page'],
  ['/sync', '.page'],
  ['/engine', '.page'],
  ['/ops-git', '.page'],
  ['/mydata', '.page'],
  ['/datacenter', '.page'],
  ['/permapproval', '.page'],
  ['/apilog', '.page'],
  ['/form', '.page'],
  ['/func', '.page'],
  ['/globalvar', '.page'],
  ['/dependency', '.page'],
  ['/work', '.page'],
  ['/realtime', '.page'],
  ['/approval', '.page'],
  ['/file-center', '.page'],
  ['/meta', '.page'],
  ['/preference', '.page'],
  ['/view', '.page'],
  ['/passwordless', '.page'],
]

async function loginViaApi() {
  const r = await fetch(BACKEND + '/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: USER, password: PASSWORD }),
  })
  const j = await r.json()
  if (r.status !== 200 || j.code !== 0 || !j.data?.token) throw new Error('backend login failed: ' + JSON.stringify(j).slice(0, 200))
  return j.data
}

async function startChrome() {
  try {
    const p = await fetch(`http://127.0.0.1:${CDP_PORT}/json/version`)
    if (p.ok) { const k = spawn('pkill', ['-f', `remote-debugging-port=${CDP_PORT}`]); await new Promise(r => k.on('exit', r)); await sleep(800) }
  } catch {}
  const profile = `/tmp/ui-smoke-${process.pid}`
  const args = ['--headless=new','--disable-gpu','--no-sandbox','--no-first-run','--no-proxy-server',`--user-data-dir=${profile}`,`--remote-debugging-port=${CDP_PORT}`,'about:blank']
  spawn(CHROME, args, { stdio: 'ignore', detached: true }).unref()
  await waitFor(async () => { try { return (await fetch(`http://127.0.0.1:${CDP_PORT}/json/version`)).ok } catch { return false } }, 9000, 400)
}

async function drive() {
  const targets = await fetch(`http://127.0.0.1:${CDP_PORT}/json`).then(r => r.json())
  const page = targets.find(t => t.type === 'page')
  if (!page) { fail('cdp.page', 'no page target'); return false }
  const ws = new WebSocket(page.webSocketDebuggerUrl)
  let idc = 0; const pending = new Map()
  ws.onmessage = e => { const m = JSON.parse(e.data); if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id) } }
  await new Promise(r => ws.onopen = r)
  const send = (method, params = {}) => new Promise(r => { const id = ++idc; pending.set(id, r); ws.send(JSON.stringify({ id, method, params })) })
  const ev = async e => { const m = await send('Runtime.evaluate', { expression: e, returnByValue: true }); return m.result && m.result.result && m.result.result.value }
  await send('Runtime.enable'); await send('Page.enable')

  const loginData = await loginViaApi().catch(e => { fail('env.login', e.message); return null })
  if (!loginData) { ws.close(); return false }

  // Real login flow: go to /login (no session), fill the form and submit.
  await send('Page.navigate', { url: FRONTEND + '/#/login' }); await sleep(2200)
  const filled = await ev(`(()=>{const ins=document.querySelectorAll('.login-page input');if(ins.length<2)return 'no-inputs:'+ins.length;const set=(el,v)=>{const s=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;s.call(el,v);el.dispatchEvent(new Event('input',{bubbles:true}))};set(ins[0],'${USER}');set(ins[1],'${PASSWORD}');return 'filled'})()`)
  check('ui.login_inputs', filled === 'filled', filled)
  await sleep(400)
  const clicked = await ev(`(()=>{const b=Array.from(document.querySelectorAll('.login-page button')).find(x=>String(x.textContent||'').includes('登录'));if(b){b.click();return 'clicked'}return 'no-btn'})()`)
  check('ui.login_submit', clicked === 'clicked', clicked)
  const redirected = await waitFor(async () => { const h = await ev('(()=>location.hash)()'); return h.startsWith('#/dashboard') }, 10000, 400)
  check('ui.login_redirect', !!redirected, 'hash=' + await ev('(()=>location.hash)()'))
  await sleep(3000)

  const shellStr = ev(`(()=>{const q=s=>document.querySelector(s);return JSON.stringify({sider:!!q('.sider'),topbar:!!q('.topbar'),nav:document.querySelectorAll('.nav-link').length,user:(q('.user-name')||{textContent:''}).textContent||''})})()`)
  const shell = JSON.parse(await shellStr)
  check('ui.shell', shell.sider && shell.topbar && shell.nav === PAGES.length, `nav=${shell.nav} user=${shell.user} expect=${PAGES.length}`)
  const statsOk = await waitFor(async () => (await ev(`(()=>document.querySelectorAll('.stat-grid .stat-card').length)()`) === 8), 10000, 500)
  check('ui.dashboard_stats', !!statsOk, '8 stat cards')

  const shot = await send('Page.captureScreenshot', { format: 'png', fromSurface: true })
  if (shot.result && shot.result.data) { const f = path.join(OUT, 'ui-dashboard.png'); fs.writeFileSync(f, Buffer.from(shot.result.data, 'base64')); console.log('UI_SHOT ' + f) }

  // Walk every page route (poll for mount — first visit compiles the chunk in dev).
  for (const [hash, rootSel] of PAGES) {
    await send('Page.navigate', { url: FRONTEND + '/#/' + hash.replace(/^\//, '') }); await sleep(1500)
    const mounted = await waitFor(async () => await ev(`(()=>!!document.querySelector('${rootSel}'))()`), 18000, 600)
    const dbg = await ev(`(()=>JSON.stringify({hash:location.hash,app:document.querySelector('#app')?.children.length||0,text:(document.querySelector('#app')?.innerText||'').slice(0,160),errs:(window.__errs||[]).slice(0,3)}))()`)
    check(`ui.mount ${hash}`, !!mounted, `sel=${rootSel} dbg=${dbg}`)
  }

  // Extra: lineage canvas G6 presence
  await send('Page.navigate', { url: FRONTEND + '/#/lineage' }); await sleep(5000)
  const g6 = await ev(`(()=>{const c=document.querySelector('.lineage-canvas');return JSON.stringify({canvas:!!(c&&c.querySelector('canvas')),errs:(window.__errs||[]).slice(0,8)})})()`)
  const gg = JSON.parse(g6)
  check('ui.lineage_g6', gg.canvas, `canvas=${gg.canvas}`)

  const errsRaw = await ev(`(()=>JSON.stringify(window.__errs||[]))()`)
  const errArr = JSON.parse(errsRaw || '[]')
  check('ui.no_console_errors', errArr.length === 0, String(errsRaw).slice(0, 300))

  ws.close()
  return true
}

;(async () => {
  await startChrome()
  const ok = await drive()
  const failed = results.filter(r => !r.pass).length
  console.log(`UI_SUMMARY pass=${results.length - failed} fail=${failed} total=${results.length}`)
  process.exit(failed ? 1 : 0)
})().catch(e => { console.error('UI_ERR', e && e.message || String(e)); process.exit(1) })

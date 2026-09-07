#!/usr/bin/env node
/**
 * Reusable real-browser walkthrough for TemporeData's lineage UI (/#/lineage).
 *
 * Drives an actual headless Chrome over the raw DevTools Protocol (no Playwright/Puppeteer
 * dependency — uses Node's built-in global WebSocket + Chrome's --remote-debugging-port).
 *
 * What it asserts:
 *   1. The lineage page mounts in its new V2 4-block layout.
 *   2. An admin session resolves `canWrite` = true → the write entry button is ENABLED
 *      and the statusbar `LINEAGE:WRITE` chip is visible.
 *   3. (If --root given) searching & selecting that root renders the G6 graph (stats show
 *      "节点 N / 连线 M").
 *   4. (If --open-search) opening the root-search dropdown hides the canvas stats overlay
 *      (regression guard for the search/dropdown overlap polish).
 *   5. No uncaught JS errors / unhandled rejections.
 *
 * Prints structured `WALK_OK`/`WALK_FAIL` lines and writes a screenshot; exits 0/1 for CI.
 *
 * Usage:
 *   node scripts/lineage-walkthrough.cjs [--root <nodeId>] [--open-search] [--out <dir>]
 *     [--frontend http://localhost:5174] [--backend http://localhost:8080]
 *     [--username admin] [--password admin123] [--chrome <path>]
 *
 * Environment note: this connects to localhost (backend + frontend + CDP loopback), so it
 * must run WITHOUT a network sandbox that blocks loopback (e.g. sandbox-disabled shell).
 */
const fs = require('fs')
const path = require('path')
const { spawn } = require('child_process')

const ARGS = (() => {
  const a = process.argv.slice(2); const o = { openSearch: false }
  const k = ['root','out','frontend','backend','username','password','chrome']
  for (let i = 0; i < a.length; i++) {
    if (a[i] === '--open-search') { o.openSearch = true; continue }
    if (a[i].startsWith('--') && k.includes(a[i].slice(2)) && a[i + 1] != null) o[a[i].slice(2)] = a[++i]
  }
  return o
})()
const FRONTEND = ARGS.frontend || 'http://localhost:5174'
const BACKEND = ARGS.backend || 'http://localhost:8080'
const USER = ARGS.username || 'admin'
const PASSWORD = ARGS.password || 'admin123'
function resolveChrome() {
  if (ARGS.chrome) return ARGS.chrome
  const cands = []
  if (process.env.CHROME_BIN) cands.push(process.env.CHROME_BIN)
  if (process.platform === 'darwin') cands.push('/Applications/Google Chrome.app/Contents/MacOS/Google Chrome')
  cands.push('/usr/bin/google-chrome', '/usr/bin/google-chrome-stable', '/usr/bin/chromium', '/usr/bin/chromium-browser')
  for (const c of cands) { if (fs.existsSync(c)) return c }
  return ARGS.chrome || 'google-chrome' // fall back to PATH lookup, may fail with a clear spawn error
}
const CHROME = resolveChrome()
const OUT = ARGS.out || '.'
const ROOT = ARGS.root || null
const OPEN_SEARCH = !!ARGS.openSearch
const CDP_PORT = 9222

const sleep = (ms) => new Promise(r => setTimeout(r, ms))
const results = []
function check(name, pass, detail = '') { results.push({ name, pass: !!pass, detail }); console.log(`WALK_${pass ? 'OK' : 'FAIL'} ${name} ${detail || ''}`) }
function fail(name, detail = '') { check(name, false, detail) }

async function waitFor(fn, ms, step) { const t0 = Date.now(); while (Date.now() - t0 < ms) { try { if (await fn()) return true } catch (e) { /* retry */ } await sleep(step || 300) } return false }

async function json(url, opts) { const r = await fetch(url, opts || {}); return { code: r.status, body: await r.json().catch(() => null) } }

async function checkEnv() {
  // backend + login
  const h = await json(BACKEND + '/actuator/health').catch(() => ({ code: 0, body: null }))
  if (h.code !== 200) { fail('env.backend', `health=${h.code}`); return null }
  const lg = await json(BACKEND + '/api/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: USER, password: PASSWORD }) })
  if (lg.code !== 200 || !lg.body || lg.body.code !== 0 || !lg.body.data?.token) { fail('env.login', `http=${lg.code} body=${JSON.stringify(lg.body)}`); return null }
  check('env.backend', true, '8080 health 200')
  check('env.login', true, `${USER} authenticated`)
  return lg.body.data
}

async function startChrome() {
  // Always a fresh instance + fresh profile so exactly one page target exists.
  try { const p = await fetch(`http://127.0.0.1:${CDP_PORT}/json/version`); if (p.ok) { const kill = spawn('pkill', ['-f', `remote-debugging-port=${CDP_PORT}`]); await new Promise(r => kill.on('exit', r)); await sleep(1000) } } catch (e) { /* none running */ }
  const profile = `/tmp/lg-walk-profile-${process.pid}`
  const args = [ '--headless=new', '--disable-gpu', '--no-sandbox', '--no-first-run', '--no-proxy-server', `--user-data-dir=${profile}`, `--remote-debugging-port=${CDP_PORT}`, 'about:blank' ]
  spawn(CHROME, args, { stdio: 'ignore', detached: true }).unref()
  await waitFor(async () => { try { return (await fetch(`http://127.0.0.1:${CDP_PORT}/json/version`)).ok } catch (e) { return false } }, 9000, 400)
  return true
}

async function drive() {
  const targets = await fetch(`http://127.0.0.1:${CDP_PORT}/json`).then(r => r.json())
  const page = targets.find(t => t.type === 'page'); if (!page) { fail('cdp.page', 'no page target'); return false }
  const ws = new WebSocket(page.webSocketDebuggerUrl)
  let idc = 0; const pending = new Map()
  ws.onmessage = e => { const m = JSON.parse(e.data); if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id) } }
  await new Promise(r => ws.onopen = r)
  const send = (method, params = {}) => new Promise(r => { const id = ++idc; pending.set(id, r); ws.send(JSON.stringify({ id, method, params })) })
  const ev = async e => { const m = await send('Runtime.evaluate', { expression: e, returnByValue: true }); return m.result && m.result.result && m.result.result.value }
  await send('Runtime.enable'); await send('Page.enable')

  const loginData = await checkEnv(); if (!loginData) { ws.close(); return false }
  const user = { userId: 'u-walk', username: USER, tenantId: 't-demo', tenantName: 't-demo', roles: ['ROLE_ADMIN'], permissions: [], admin: true, nickname: null }

  await send('Page.navigate', { url: FRONTEND + '/#/login' }); await sleep(1500)
  await ev(`(()=>{localStorage.setItem('td_token',${JSON.stringify(loginData.token)});localStorage.setItem('td_user',${JSON.stringify(JSON.stringify(user))});window.__errs=[];window.addEventListener('error',e=>window.__errs.push('ERR:'+(e.message||e)));window.addEventListener('unhandledrejection',e=>window.__errs.push('REJ:'+String(e.reason)));return 1})()`)
  await send('Page.reload'); await sleep(3500)                       // re-init Pinia with the admin user
  await send('Page.navigate', { url: FRONTEND + '/#/lineage' }); await sleep(3000)

  await waitFor(async () => await ev(`(()=>!!document.querySelector('.lineage-v2-page'))()`), 6000, 500)

  const mount = await ev(`(()=>{const q=s=>document.querySelector(s);return JSON.stringify({v2:!!q('.lineage-v2-page'),tree:!!q('.lv2-tree'),toolbar:!!q('.lv2-toolbar'),body:!!q('.lv2-body'),detail:!!q('.lv2-detail'),status:!!q('.lv2-status')})})()`)
  const m = JSON.parse(mount)
  check('ui.mount_v2', m.v2 && m.tree && m.toolbar && m.body && m.detail && m.status, `v2=${m.v2} 4block=${m.tree&&m.toolbar&&m.body&&m.detail&&m.status}`)

  const gating = await ev(`(()=>{const b=Array.from(document.querySelectorAll('button')).find(x=>String(x.textContent||'').includes('添加血缘'));return JSON.stringify({chip:!!document.querySelector('.st-chip.st-write'),disabled:b?b.disabled:null,aria:b?b.getAttribute('aria-disabled'):null,isDisabled:b?b.className.includes('is-disabled'):null})})()`)
  const g = JSON.parse(gating)
  check('auth.admin_canWrite', g.chip && g.disabled === false && g.aria === 'false', `chip=${g.chip} disabled=${g.disabled} aria=${g.aria}`)

  if (OPEN_SEARCH) {
    // regression guard: opening the root-search dropdown hides the canvas stats overlay
    await ev(`(()=>{const i=document.querySelector('.tb-search input');i&&i.focus();return 1})()`); await sleep(250)
    const stat = await ev(`(()=>{const s=document.querySelector('.lv2-graph-stats');return s?getComputedStyle(s).display:'no-stat'})()`)
    check('ui.search_hides_stats_overlay', stat === 'none' || stat === 'no-stat', `stats display=${stat}`)
    await send('Input.press', { key: 'Escape' }); await sleep(200)
  }

  if (ROOT) {
    // Open the root-search dropdown explicitly (typing into a closed el-select does not
    // necessarily trigger its remote method), then type, then select the matching option.
    const opened = await ev(`(()=>{const w=document.querySelector('.tb-search .el-select__wrapper')||document.querySelector('.tb-search');w&&w.click();return !!w})()`)
    await sleep(400)
    const inp = await ev(`(()=>{const i=document.querySelector('.tb-search input');i&&i.focus();return !!i})()`)
    await sleep(200); await send('Input.insertText', { text: ROOT.replace(/^.*:/, '') })
    const shortName = ROOT.replace(/^.*:/, '')
    await waitFor(async () => { const o = await ev(`(()=>Array.from(document.querySelectorAll('.el-select-dropdown__item')).map(x=>String(x.textContent||'').trim()).some(t=>t.startsWith('${shortName}')))()`); return o }, 6000, 300)
    const dbg = await ev(`(()=>JSON.stringify(Array.from(document.querySelectorAll('.el-select-dropdown__item')).map(o=>String(o.textContent||'').trim()).filter(t=>t.startsWith('${shortName}')).slice(0,3)))()`)
    console.log('DBG rootOpts=' + dbg)
    const picked = await ev(`(()=>{let t=null;document.querySelectorAll('.el-select-dropdown__item').forEach(o=>{if(String(o.textContent||'').trim().startsWith('${shortName}'))t=o});if(t){t.click();return 'clicked'}return 'no'})()`)
    console.log('DBG picked=' + picked)
    await sleep(3500)
    const graph = await ev(`(()=>{const s=document.querySelector('.lv2-graph-stats');return JSON.stringify({picked:${picked==='clicked'},g6:!!document.querySelector('.lv2-canvas canvas'),stats:s?s.innerText.replace(/\\n/g,' | '):'',errs:window.__errs||[]})})()`)
    const gg = JSON.parse(graph)
    check('ui.graph_render', gg.picked && gg.g6 && /节点\s*\d+/.test(gg.stats), `g6=${gg.g6} stats=${gg.stats}`)
    if (gg.errs && gg.errs.length) fail('ui.no_console_errors', gg.errs.join('; ')); else check('ui.no_console_errors', true, `${(gg.errs||[]).length} errors`)
  }

  const errs = await ev(`(()=>JSON.stringify(window.__errs||[]))()`)
  if (!ROOT) { if (errs && errs.length > 2) fail('ui.no_console_errors', errs.slice(0,200)); else check('ui.no_console_errors', true, `${String(errs).length} errors`) }

  const shot = await send('Page.captureScreenshot', { format: 'png', fromSurface: true })
  if (shot.result && shot.result.data) { const f = path.join(OUT, 'lineage-walkthrough.png'); fs.writeFileSync(f, Buffer.from(shot.result.data, 'base64')); console.log('WALK_SHOT ' + f) }
  ws.close(); return true
}

;(async () => {
  await startChrome()
  const ok = await drive()
  const failed = results.filter(r => !r.pass).length
  console.log(`WALK_SUMMARY pass=${results.length - failed} fail=${failed} total=${results.length}`)
  process.exit(failed ? 1 : 0)
})().catch(e => { console.error('WALK_ERR', e && e.message || String(e)); process.exit(1) })
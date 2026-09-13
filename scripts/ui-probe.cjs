#!/usr/bin/env node
// Probe a single route: dump DOM state, innerText head, and console errors.
const { spawn } = require('child_process')
const FRONTEND = process.argv[2] || 'http://localhost:5173'
const ROUTE = process.argv[3] || '/catalog'
const CDP_PORT = 9225
const sleep = ms => new Promise(r => setTimeout(r, ms))
const waitFor = async (fn, ms, step) => { const t0 = Date.now(); while (Date.now() - t0 < ms) { try { if (await fn()) return true } catch {} await sleep(step || 300) } return false }

;(async () => {
  try { const p = await fetch(`http://127.0.0.1:${CDP_PORT}/json/version`); if (p.ok) { spawn('pkill', ['-f', `remote-debugging-port=${CDP_PORT}`]); await sleep(800) } } catch {}
  const profile = `/tmp/ui-probe-${process.pid}`
  spawn('/Applications/Google Chrome.app/Contents/MacOS/Google Chrome', ['--headless=new','--disable-gpu','--no-sandbox','--no-first-run','--no-proxy-server',`--user-data-dir=${profile}`,`--remote-debugging-port=${CDP_PORT}`,'about:blank'], { stdio: 'ignore', detached: true }).unref()
  await waitFor(async () => { try { return (await fetch(`http://127.0.0.1:${CDP_PORT}/json/version`)).ok } catch { return false } }, 9000, 400)
  const targets = await fetch(`http://127.0.0.1:${CDP_PORT}/json`).then(r => r.json())
  const page = targets.find(t => t.type === 'page')
  const ws = new WebSocket(page.webSocketDebuggerUrl)
  let idc = 0; const pending = new Map()
  ws.onmessage = e => { const m = JSON.parse(e.data); if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id) } }
  await new Promise(r => ws.onopen = r)
  const send = (method, params = {}) => new Promise(r => { const id = ++idc; pending.set(id, r); ws.send(JSON.stringify({ id, method, params })) })
  const ev = async e => { const m = await send('Runtime.evaluate', { expression: e, returnByValue: true }); return m.result && m.result.result && m.result.result.value }
  await send('Runtime.enable'); await send('Page.enable')
  await send('Page.navigate', { url: FRONTEND + '/#/login' }); await sleep(2000)
  // Login via real form
  await ev(`(()=>{window.__errs=[];window.addEventListener('error',e=>window.__errs.push('ERR:'+(e.message||e)));window.addEventListener('unhandledrejection',e=>window.__errs.push('REJ:'+String(e.reason)));return 1})()`)
  await ev(`(()=>{const ins=document.querySelectorAll('.login-page input');if(ins.length<2)return 'no-inputs:'+ins.length;const set=(el,v)=>{const s=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;s.call(el,v);el.dispatchEvent(new Event('input',{bubbles:true}))};set(ins[0],'admin');set(ins[1],'admin123');return 'filled'})()`)
  await sleep(400)
  await ev(`(()=>{const b=Array.from(document.querySelectorAll('.login-page button')).find(x=>String(x.textContent||'').includes('登录'));b&&b.click();return 1})()`)
  await sleep(4000)
  await send('Page.navigate', { url: FRONTEND + '/#/' + ROUTE }); await sleep(8000)
  const st = await ev(`(()=>JSON.stringify({hash:location.hash,app:document.querySelector('#app')?.children.length||0,html:(document.querySelector('#app')?.innerText||'').slice(0,400),errs:(window.__errs||[]).slice(0,6)}))()`)
  console.log(st)
  ws.close()
  process.exit(0)
})().catch(e => { console.error('PROBE_ERR', e.message); process.exit(1) })

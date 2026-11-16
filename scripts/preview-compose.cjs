// Displays real simulator captures and shared Compose renders. It does not simulate the app.
const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(process.argv[2] || 'docs/screenshots');
const port = Number(process.env.GEEFLOW_PREVIEW_PORT || 4181);
const font = path.resolve(__dirname, '../shared/core/ui/src/commonMain/composeResources/font/Pretendard_Regular.otf');
const names = new Set(['tablet-1.png', 'tablet-2.png', 'tablet-3.png', 'tablet-night-1.png', 'tablet-night-2.png', 'tablet-night-3.png', 'tablet-1024.png', 'tablet-1024-night.png', 'phone-1.png', 'phone-2.png', 'phone-3.png', 'tablet-temperature.png', 'tablet-temperature-night.png', 'ipad-dashboard-day.png', 'ipad-dashboard-night.png', 'ipad-temperature-day.png', 'ipad-temperature-night.png']);
const html = `<!doctype html><html lang="ko"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>GeeFlow · 앱 화면 검토</title>
<style>@font-face{font-family:Pretendard;src:url('/font.otf')}*{box-sizing:border-box}body{margin:0;background:#080d15;color:#e7edfa;font:14px Pretendard,system-ui}header{display:flex;align-items:center;gap:20px;padding:18px 24px;border-bottom:1px solid #ffffff16;flex-wrap:wrap}strong{font-size:17px;margin-right:auto}nav{display:flex;gap:4px;background:#ffffff09;border-radius:24px;padding:4px}button{border:0;border-radius:20px;background:none;color:#a0b1c9;padding:9px 14px;font:inherit;cursor:pointer}button[aria-pressed=true]{background:#e1eaff;color:#122138}button:focus-visible{outline:2px solid #88b4ff}main{padding:24px;display:grid;place-items:center;min-height:70vh}img{max-width:100%;max-height:calc(100vh - 160px);border-radius:18px;box-shadow:0 28px 90px #0008}footer{color:#8496b1;text-align:center;font-size:12px;padding:0 20px 18px}#pending{padding:90px 0;color:#8496b1}</style>
<header><strong>GeeFlow</strong><nav aria-label="화면"><button data-screen="1" aria-pressed="true">추출</button><button data-screen="4" aria-pressed="false">온도</button><button data-screen="2" aria-pressed="false">레시피</button><button data-screen="3" aria-pressed="false">단계</button></nav><nav aria-label="시간"><button data-theme="day" aria-pressed="true">낮</button><button data-theme="night" aria-pressed="false">밤</button></nav></header>
<main><div id="pending">새 앱 화면을 렌더링하고 있습니다.</div><img id="capture" hidden alt="GeeFlow 앱 화면, 한국어, 가로 iPad"></main><footer><span id="source"></span> · 데모 데이터 · 한국어 / Pretendard<span id="revision"></span></footer>
<script>
let screen='1',theme='day',version='',native=false;
const capture=document.querySelector('#capture'),pending=document.querySelector('#pending');
function show(){
  const isNative=native&&(screen==='1'||screen==='4');
  const name=isNative?'ipad-'+(screen==='1'?'dashboard':'temperature')+'-'+theme+'.png':screen==='4'?'tablet-temperature'+(theme==='night'?'-night':'')+'.png':(theme==='night'?'tablet-night-':'tablet-')+screen+'.png';
  document.querySelector('#source').textContent=isNative?'iPad 시뮬레이터 · 네이티브 UIKit':'공유 Compose · JVM 렌더';
  capture.src='/'+name+'?v='+encodeURIComponent(version);
}
capture.onload=()=>{capture.hidden=false;pending.hidden=true};
capture.onerror=()=>{capture.hidden=true;pending.hidden=false};
document.querySelectorAll('button').forEach(b=>b.onclick=()=>{
  if(b.dataset.screen)screen=b.dataset.screen;
  if(b.dataset.theme)theme=b.dataset.theme;
  document.querySelectorAll('[data-screen]').forEach(n=>n.setAttribute('aria-pressed',n.dataset.screen===screen));
  document.querySelectorAll('[data-theme]').forEach(n=>n.setAttribute('aria-pressed',n.dataset.theme===theme));show();
});
async function refresh(){try{
  const s=await fetch('/version').then(r=>r.json());
  if(s.version&&s.version!==version){version=s.version;native=s.native;document.querySelector('#revision').textContent=s.commit?' · '+s.commit:'';show()}
}catch{}}
refresh();setInterval(refresh,5000);
</script></html>`;
http.createServer((req, res) => {
  const route = new URL(req.url, 'http://localhost').pathname;
  res.setHeader('Cache-Control', 'no-store');
  if (route === '/') { res.setHeader('Content-Type', 'text/html; charset=utf-8'); return res.end(html); }
  if (route === '/version') {
    res.setHeader('Content-Type', 'application/json');
    const native = ['ipad-dashboard-day.png', 'ipad-dashboard-night.png', 'ipad-temperature-day.png', 'ipad-temperature-night.png'].every(name => fs.existsSync(path.join(root, name)));
    const first = path.join(root, native ? 'ipad-dashboard-day.png' : 'tablet-1.png');
    const metadata = path.join(root, 'latest.json');
    let info = {};
    try { info = JSON.parse(fs.readFileSync(metadata, 'utf8')); } catch {}
    return res.end(JSON.stringify({...info, native, version: fs.existsSync(first) ? String(fs.statSync(first).mtimeMs) : ''}));
  }
  const name = route.slice(1);
  const file = route === '/font.otf' ? font : names.has(name) ? path.join(root, name) : null;
  if (!file || !fs.existsSync(file)) { res.writeHead(404); return res.end(); }
  res.setHeader('Content-Type', route === '/font.otf' ? 'font/otf' : 'image/png');
  fs.createReadStream(file).pipe(res);
}).listen(port, '127.0.0.1', () => process.stdout.write('Compose review: http://127.0.0.1:' + port + '\n'));

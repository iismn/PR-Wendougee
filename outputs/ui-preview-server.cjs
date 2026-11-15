const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, 'ui-preview');
const port = Number(process.env.CREMA_PREVIEW_PORT || 4180);
const version = () => {
  try { const s = fs.statSync(path.join(root, 'index.html')); return `${s.mtimeMs}-${s.size}`; }
  catch { return 'waiting'; }
};
const refresh = `<script>(()=>{let v;setInterval(async()=>{try{const n=await(await fetch('/__version',{cache:'no-store'})).text();if(v&&n!==v)location.reload();v=n;}catch{}},1200)})();</script>`;
http.createServer((req, res) => {
  res.setHeader('Cache-Control', 'no-store');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  if (req.url === '/__version') { res.setHeader('Content-Type', 'text/plain'); res.end(version()); return; }
  if (req.url?.split('?')[0] !== '/') { res.writeHead(404); res.end('Not found'); return; }
  res.setHeader('Content-Type', 'text/html; charset=utf-8');
  const file = path.join(root, 'index.html');
  if (fs.existsSync(file)) { res.end(fs.readFileSync(file, 'utf8') + refresh); return; }
  res.end(`<!doctype html><html lang="ko"><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>Crema · Live design</title><style>body{margin:0;background:#f3f0e9;color:#29251f;font:16px system-ui;padding:32px}small{letter-spacing:3px;color:#756e63}h1{font-weight:500;font-size:44px}section{background:#29251f;color:#f8f3ea;border-radius:30px;padding:32px;margin-top:40px}p{line-height:1.7;color:#756e63}section p{color:#e9ad68}</style><small>CREMA / BREW STUDIO</small><h1>커피를 위한 새로운 화면.</h1><section><small>LIVE DESIGN PREVIEW</small><h1>00.0 <span style="font-size:16px">s</span></h1><p>☕ Porcelain · Espresso · Caramel · Sage</p></section><p>구현된 SwiftUI 화면의 미리보기를 준비하고 있습니다.<br>파일이 저장되면 이 패널이 자동으로 갱신됩니다.</p>${refresh}</html>`);
}).listen(port, '127.0.0.1', () => console.log(`Crema design preview: http://127.0.0.1:${port}`));

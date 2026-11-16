// Local review gallery of real Compose captures, never a simulated machine controller.
const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '../docs/review/all-ui');
const html = path.resolve(__dirname, 'review-gallery.html');
const font = path.resolve(__dirname, '../shared/core/ui/src/commonMain/composeResources/font/Pretendard_Regular.otf');
const port = Number(process.env.GEEFLOW_REVIEW_PORT || 4182);
http.createServer((req, res) => {
  const route = new URL(req.url, 'http://localhost').pathname;
  const name = route.slice(1);
  let file, type;
  if (route === '/') { file = html; type = 'text/html; charset=utf-8'; }
  else if (route === '/font.otf') { file = font; type = 'font/otf'; }
  else if (route === '/manifest.json') { file = path.join(root, name); type = 'application/json; charset=utf-8'; }
  else if (/^[a-z0-9-]+-(day|night)\.png$/.test(name)) { file = path.join(root, name); type = 'image/png'; }
  if (!file || !fs.existsSync(file)) { res.writeHead(404); return res.end('Not found'); }
  res.setHeader('Content-Type', type);
  res.setHeader('Cache-Control', 'no-store');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  fs.createReadStream(file).pipe(res);
}).listen(port, '127.0.0.1', () => console.log(`All UI review: http://127.0.0.1:${port}`));

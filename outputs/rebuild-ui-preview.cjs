// Inline the editable stylesheet so index.html remains an offline, single-file preview.
const fs = require('node:fs');
const path = require('node:path');
const root = path.join(__dirname, 'ui-preview');
const file = path.join(root, 'index.html');
const html = fs.readFileSync(file, 'utf8');
const font = html.match(/@font-face\{[^}]+\}/)?.[0];
if (!font) throw new Error('Embedded Pretendard declaration missing');
const css = fs.readFileSync(path.join(root, 'crema.css'), 'utf8');
fs.writeFileSync(file, html.replace(/<style>[\s\S]*?<\/style>/, `<style>\n${font}\n${css}\n</style>`));
console.log('Updated self-contained Crema preview stylesheet.');

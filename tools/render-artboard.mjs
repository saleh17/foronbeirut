// A minimal renderer for .dc.html artboards, so the design can be rasterised
// locally and used as the app's art instead of being re-drawn by hand.
import fs from 'node:fs';
import { chromium } from 'playwright-core';

const src = fs.readFileSync(process.argv[2], 'utf8');
const out = process.argv[3];

const helmet = (src.match(/<helmet>([\s\S]*?)<\/helmet>/) || [, ''])[1];
const body = src.slice(src.indexOf('</helmet>') + 9, src.lastIndexOf('</x-dc>'));
const script = (src.match(/<script data-dc-script[^>]*>([\s\S]*?)<\/script>/) || [, ''])[1];
const props = JSON.parse((src.match(/data-props='([^']*)'/) || [, '{}'])[1]);
const defaults = {};
for (const [k, v] of Object.entries(props)) if (v && typeof v === 'object' && 'default' in v) defaults[k] = v.default;

// run the component to get its render values
const DCLogic = `class DCLogic {
  constructor(props){ this.props = props || {}; this.state = {}; }
  setState(p){ Object.assign(this.state, p); }
}`;
const vals = await (async () => {
  const over = process.env.STATE ? JSON.parse(process.env.STATE) : {};
  const mod = `${DCLogic}\n${script}\nexport const mk = (o) => { const c = new Component(${JSON.stringify(defaults)}); Object.assign(c.state, o); return typeof c.renderVals === 'function' ? c.renderVals() : {}; };`;
  return (await import('data:text/javascript;base64,' + Buffer.from(mod).toString('base64'))).mk(over);
})();
const _unused = (async () => {
  const mod = ``;
  return null;
})();

const look = (scope, path) => path.split('.').reduce((o, k) => (o == null ? undefined : o[k]), scope);

function findBlock(s, tag, from) {
  const open = new RegExp(`<${tag}\\b[^>]*>`, 'g');
  open.lastIndex = from;
  const m = open.exec(s);
  if (!m) return null;
  let depth = 1, i = m.index + m[0].length;
  const scan = new RegExp(`<${tag}\\b[^>]*>|</${tag}>`, 'g');
  scan.lastIndex = i;
  let t;
  while ((t = scan.exec(s))) {
    depth += t[0].startsWith('</') ? -1 : 1;
    if (depth === 0) return { start: m.index, tagEnd: m.index + m[0].length, inner: [i, t.index], end: t.index + t[0].length, attrs: m[0] };
  }
  return null;
}

function expand(tpl, scope) {
  let s = tpl, b;
  while ((b = findBlock(s, 'sc-for', 0))) {
    const list = look(scope, (b.attrs.match(/list="\{\{([^}]*)\}\}"/) || [, ''])[1]) || [];
    const as = (b.attrs.match(/as="([^"]*)"/) || [, 'it'])[1];
    const inner = s.slice(b.inner[0], b.inner[1]);
    s = s.slice(0, b.start) + list.map(item => expand(inner, { ...scope, [as]: item })).join('') + s.slice(b.end);
  }
  while ((b = findBlock(s, 'sc-if', 0))) {
    const on = look(scope, (b.attrs.match(/value="\{\{([^}]*)\}\}"/) || [, ''])[1]);
    const inner = s.slice(b.inner[0], b.inner[1]);
    s = s.slice(0, b.start) + (on ? expand(inner, scope) : '') + s.slice(b.end);
  }
  return s.replace(/\{\{([^}]*)\}\}/g, (_, p) => {
    const v = look(scope, p.trim());
    return v == null || typeof v === 'function' || typeof v === 'object' ? '' : String(v);
  });
}

let html = expand(body, vals).replace(/\son(Click|PointerDown|PointerMove|PointerUp|PointerCancel)="[^"]*"/g, '');
const w = props.$preview?.width || 844, h = props.$preview?.height || 390;
const page_html = `<!doctype html><html><head><meta charset="utf-8">${helmet}
<style>html,body{margin:0;padding:0}#stage{width:${w}px;height:${h}px;position:relative;overflow:hidden}</style>
</head><body><div id="stage">${html}</div></body></html>`;
fs.writeFileSync('rendered.html', page_html);

const exe = fs.readdirSync('/opt/pw-browsers').find(d => d.startsWith('chromium-'));
const browser = await chromium.launch({ executablePath: `/opt/pw-browsers/${exe}/chrome-linux/chrome`, args: ['--no-sandbox', '--disable-gpu'] });
const page = await browser.newPage({ viewport: { width: w, height: h }, deviceScaleFactor: Number(process.argv[4] || 3) });
page.on('pageerror', e => console.log('PAGE ERR', String(e).slice(0, 140)));
await page.setContent(page_html, { waitUntil: 'networkidle' });
await page.waitForTimeout(2500);
await page.locator('#stage').screenshot({ path: out });
const boxes = await page.evaluate(() => {
  const stage = document.getElementById('stage').getBoundingClientRect();
  const out = {};
  document.querySelectorAll('#stage *').forEach((el) => {
    const t = (el.textContent || '').trim().slice(0, 22);
    const s = el.getAttribute('style') || '';
    if (!/position:\s*absolute/.test(s)) return;
    const r = el.getBoundingClientRect();
    if (r.width < 24 || r.height < 16) return;
    const key = t.replace(/\s+/g, ' ');
    if (!key) return;
    const box = [Math.round(r.left - stage.left), Math.round(r.top - stage.top), Math.round(r.width), Math.round(r.height)];
    if (!out[key]) out[key] = box;
  });
  return out;
});
fs.writeFileSync(out.replace(/\.png$/, '.json'), JSON.stringify(boxes, null, 1));
console.log('wrote', out);
await browser.close();

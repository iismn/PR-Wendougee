// Run on CI's isolated Chromium; never connects to the user's browser or machine.
const fs = require('node:fs/promises');
const path = require('node:path');
const assert = require('node:assert/strict');
const { spawn } = require('node:child_process');
const { chromium } = require(process.env.CREMA_PLAYWRIGHT_MODULE || 'playwright');
const output = path.resolve('.build/preview-review');
const server = spawn(process.execPath, [path.join(__dirname, 'ui-preview-server.cjs')], {
  env: { ...process.env, CREMA_PREVIEW_PORT: '4189' }, stdio: ['ignore', 'pipe', 'inherit']
});

(async () => {
  await fs.mkdir(output, { recursive: true });
  await new Promise((resolve, reject) => {
    server.stdout.once('data', resolve);
    server.once('error', reject);
    server.once('exit', code => reject(new Error(`Preview server exited: ${code}`)));
  });
  const browser = await chromium.launch();
  const report = [];
  try {
    for (const [width, height, phase] of [[1194, 834, 'day'], [1194, 834, 'night'], [1024, 768, 'day'], [390, 844, 'day']]) {
      const context = await browser.newContext({ viewport: { width, height }, deviceScaleFactor: 1, reducedMotion: 'reduce' });
      const page = await context.newPage();
      const errors = [];
      page.on('pageerror', error => errors.push(error.message));
      await page.goto('http://127.0.0.1:4189/');
      await page.evaluate(() => document.fonts.ready);
      await page.selectOption('#appearance', phase);
      await page.waitForFunction(() => !!window.CremaPreview && document.querySelector('#chart').height > 0);
      const layout = await page.evaluate(() => {
        const rect = selector => {
          const r = document.querySelector(selector).getBoundingClientRect();
          return { x: r.x, y: r.y, right: r.right, bottom: r.bottom, width: r.width, height: r.height };
        };
        return { overflow: document.documentElement.scrollWidth > innerWidth + 1,
          chart: rect('#chart'), stages: rect('#journey'), dock: rect('.playback-dock'),
          metrics: Array.from(document.querySelectorAll('.hud-tile')).map(e => e.getBoundingClientRect().y),
          play: rect('#play'), rail: rect('.recipe-rail'), font: document.fonts.check('16px Pretendard') };
      });
      const name = `${width}x${height}-${phase}`;
      await page.screenshot({ path: path.join(output, `${name}.png`), fullPage: true });
      assert.equal(layout.overflow, false, `${name}: horizontal overflow`);
      assert.equal(layout.font, true, `${name}: Pretendard font missing`);
      assert.ok(layout.play.width >= 44 && layout.play.height >= 44, `${name}: primary target too small`);
      if (width > 820) {
        assert.ok(layout.metrics.every(y => Math.abs(y - layout.metrics[0]) < 1), `${name}: four metrics must share a row`);
        assert.ok(layout.stages.bottom <= layout.dock.y + 1, `${name}: stages obscured by dock`);
        assert.ok(layout.chart.height >= 210, `${name}: chart too short`);
        assert.ok(layout.rail.width >= 260, `${name}: landscape recipe library missing`);
        assert.ok(layout.chart.right < layout.rail.x, `${name}: chart overlaps recipe library`);
        assert.ok(layout.dock.right <= layout.rail.x, `${name}: dock overlaps recipe library`);
      } else {
        assert.equal(layout.rail.width, 0, `${name}: compact view must retain original library sheet`);
      }
      if (width === 1194 && phase === 'day') {
        await page.evaluate(() => { document.querySelector('#journey').firstElementChild.dataset.retained = 'true'; });
        await page.click('#play');
        await page.waitForFunction(() => window.CremaPreview.state.t > 20.8);
        await page.click('#play');
        assert.equal(await page.locator('#journey > span').first().getAttribute('data-retained'), 'true', 'Stage transitions must retain their DOM');
        await page.click('[data-rail-recipe="1"]');
        assert.equal(await page.locator('#pageTitle').textContent(), await page.evaluate(() => window.CremaPreview.recipes[1].name));
        assert.equal(await page.locator('[data-rail-recipe="1"]').getAttribute('aria-pressed'), 'true');
        assert.equal(await page.evaluate(() => window.CremaPreview.state.mode), 'live');
        assert.equal(await page.locator('#pressureValue .dot-readout').getAttribute('aria-label'), '—', 'Unconnected recipe must not display demo telemetry');
        await page.screenshot({ path: path.join(output, 'landscape-recipe-selection.png'), fullPage: true });
        await page.click('#profilePicker');
        assert.equal(await page.locator('.profile-row').count(), 2);
        await page.click('[data-edit="0"]');
        await page.locator('#editorSheet').waitFor({ state: 'visible' });
        const chart = await page.locator('#editorChart').boundingBox();
        const dial = await page.locator('#targetDial').boundingBox();
        assert.ok(chart.x + chart.width < dial.x, 'Landscape editor must show graph beside controls');
        await page.focus('#targetInput');
        for (let i = 0; i < 21; i++) await page.keyboard.press('ArrowRight');
        assert.ok(Math.abs(Number(await page.inputValue('#targetInput')) - 6.1) < 0.001, 'Dial keyboard adjustment');
        await page.screenshot({ path: path.join(output, 'landscape-editor-6.1.png'), fullPage: true });
        await page.click('[data-close="editorSheet"]');
        await page.click('[data-edit="0"]');
        assert.equal(await page.inputValue('#targetInput'), '4', 'Cancelled changes must not persist');
      }
      assert.deepEqual(errors, [], `${name}: script errors`);
      report.push({ name, ...layout, errors });
      await context.close();
    }
    await fs.writeFile(path.join(output, 'layout.json'), JSON.stringify(report, null, 2));
    console.log('PASS: four viewport/theme layouts, landscape library selection, stable replay stages, original edit flow, 6.1 bar adjustment and cancel semantics.');
  } finally {
    await browser.close();
  }
})().catch(error => { console.error(error); process.exitCode = 1; }).finally(() => server.kill());

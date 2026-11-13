import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';

const root = new URL('../', import.meta.url);
test('obsolete CRA dependency chains stay absent from every lockfile entry', () => {
  const lock = JSON.parse(readFileSync(new URL('package-lock.json', root), 'utf8'));
  const removed = ['react-scripts', 'webpack-dev-server', 'webpack-dev-middleware', 'svgo', 'uuid', 'deepmerge', '@tootallnate/once'];
  for (const path of Object.keys(lock.packages)) {
    for (const name of removed) {
      assert.ok(!path.endsWith(`node_modules/${name}`), `${name} returned at ${path}`);
    }
  }
  // This deliberately checks the resolved graph, not vendored Rsbuild code.
  // Rsbuild 2.3.2 still embeds affected deepmerge 4.3.1.
});
test('HTML preserves a unique extension marker before script injection', () => {
  const html = readFileSync(new URL('public/index.html', root), 'utf8');
  assert.equal(html.split('<!--yupiik-batch-extensions-->').length, 2);
  assert.ok(!html.includes('%PUBLIC_URL%'));
});

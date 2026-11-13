import assert from 'node:assert/strict';
import http from 'node:http';
import { gzipSync } from 'node:zlib';
import { after, before, test } from 'node:test';
import { createDevMiddleware } from '../dev/jsonrpc.mjs';

let server;
let origin;
before(async () => {
  const middleware = createDevMiddleware();
  server = http.createServer((req, res) => middleware(req, res, () => { res.writeHead(404).end(); }));
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  origin = `http://127.0.0.1:${server.address().port}`;
});
after(async () => { await new Promise(resolve => server.close(resolve)); });
const rpc = (body, headers = {}) => fetch(`${origin}/jsonrpc`, {
  method: 'POST', headers: { 'Content-Type': 'application/json', ...headers }, body: JSON.stringify(body),
});
const request = { jsonrpc: '2.0', id: 1, method: 'yupiik-batch-executions', params: { pageSize: 2 } };

test('single request and paginated fixture', async () => {
  const response = await rpc(request);
  assert.equal(response.status, 200);
  const body = await response.json();
  assert.equal(body.id, 1);
  assert.equal(body.result.items.length, 2);
  assert.ok(body.result.total > 2);
});
test('batch omits notifications and preserves IDs', async () => {
  const { id: _id, ...notification } = request;
  const body = await (await rpc([request, notification, { ...request, id: 7 }])).json();
  assert.deepEqual(body.map(it => it.id), [1, 7]);
  assert.equal((await rpc(notification)).status, 204);
});
test('invalid requests and protocol errors', async () => {
  assert.equal((await (await rpc([])).json()).error.code, -32600);
  assert.equal((await (await rpc({ ...request, method: 'missing' })).json()).error.code, -32601);
  const malformed = await fetch(`${origin}/jsonrpc`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{' });
  assert.equal((await malformed.json()).error.code, -32700);
});
test('rejects cross-origin requests, wrong methods and oversized input', async () => {
  assert.equal((await rpc(request, { Origin: 'http://evil.invalid' })).status, 403);
  const badHost = await new Promise((resolve, reject) => {
    const req = http.request(`${origin}/jsonrpc`, { method: 'POST', headers: { Host: 'evil.invalid', 'Content-Type': 'application/json' } }, res => { res.resume(); resolve(res.statusCode); });
    req.on('error', reject);
    req.end(JSON.stringify(request));
  });
  assert.equal(badHost, 403);
  assert.equal((await fetch(`${origin}/jsonrpc`)).status, 405);
  assert.equal((await fetch(`${origin}/jsonrpc`, { method: 'POST', body: '{}' })).status, 415);
  assert.equal((await rpc('x'.repeat(1024 * 1024))).status, 413);
  assert.equal((await fetch(`${origin}/elsewhere`)).status, 404);
});
test('page/pageSize and batch filtering use backend parameter semantics', async () => {
  const first = (await (await rpc({ ...request, params: { page: 0, pageSize: 2 } })).json()).result;
  const second = (await (await rpc({ ...request, params: { page: 1, pageSize: 2 } })).json()).result;
  assert.equal(second.items.length, 2);
  assert.notEqual(first.items[0].id, second.items[0].id);
  const batch = first.items[0].name;
  const filtered = (await (await rpc({ ...request, params: { batch } })).json()).result;
  assert.ok(filtered.items.every(item => item.name === batch));
  const all = (await (await rpc({ ...request, method: 'yupiik-batch-last-executions', params: {} })).json()).result;
  assert.ok(all.items.some(item => item.name === batch));
});
test('scoped live proxy preserves authentication, status and compressed responses', async () => {
  const upstream = http.createServer((req, res) => {
    assert.equal(req.url, '/jsonrpc');
    assert.equal(req.headers.authorization, 'Bearer test');
    res.writeHead(202, { 'Content-Type': 'application/json', 'Content-Encoding': 'gzip' }).end(gzipSync('{"ok":true}'));
  });
  await new Promise(resolve => upstream.listen(0, '127.0.0.1', resolve));
  const middleware = createDevMiddleware({ backend: `http://127.0.0.1:${upstream.address().port}` });
  const proxy = http.createServer((req, res) => middleware(req, res, () => res.writeHead(404).end()));
  try {
    await new Promise(resolve => proxy.listen(0, '127.0.0.1', resolve));
    const response = await fetch(`http://127.0.0.1:${proxy.address().port}/jsonrpc`, {
      method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: 'Bearer test' }, body: JSON.stringify(request),
    });
    assert.equal(response.status, 202);
    assert.deepEqual(await response.json(), { ok: true });
  } finally {
    await Promise.all([new Promise(resolve => proxy.close(resolve)), new Promise(resolve => upstream.close(resolve))]);
  }
});
test('backend proxy configuration cannot target remote hosts', () => {
  assert.throws(() => createDevMiddleware({ backend: 'https://example.com' }));
  assert.throws(() => createDevMiddleware({ backend: 'http://user@localhost:8080' }));
});

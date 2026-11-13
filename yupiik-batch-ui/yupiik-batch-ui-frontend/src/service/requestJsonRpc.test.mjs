import assert from 'node:assert/strict';
import { setImmediate } from 'node:timers/promises';
import { test } from 'node:test';
import { requestJsonRpc } from './requestJsonRpc.mjs';

const method = 'test-method';
const batch = [
    { jsonrpc: '2.0', id: 'page', method, params: {} },
    { jsonrpc: '2.0', id: 'names', method, params: {} },
];
const reply = (id, result) => ({ jsonrpc: '2.0', id, result });

function mockFetch(t, implementation) {
    t.mock.method(globalThis, 'fetch', implementation);
}

function values(actions, suffix) {
    return actions.filter(action => action.type === `${method}-${suffix}`).map(action => action.value);
}

async function run(t, implementation, params = {}) {
    mockFetch(t, implementation);
    const actions = [];
    requestJsonRpc(method, params, action => actions.push(action));
    await setImmediate();
    return actions;
}

test('single request clears stale state, sends headers and accepts null result', async t => {
    const actions = [];
    mockFetch(t, async (url, options) => {
        assert.equal(url, '/jsonrpc?m=test-method');
        assert.equal(options.headers.Authorization, 'Bearer test');
        assert.equal(options.method, 'POST');
        const request = JSON.parse(options.body);
        assert.deepEqual(request.params, { id: '123' });
        assert.equal(request.jsonrpc, '2.0');
        return { ok: true, json: async () => reply(request.id, null) };
    });
    requestJsonRpc(method, { id: '123' }, action => actions.push(action), () => ({ Authorization: 'Bearer test' }));
    await setImmediate();
    assert.deepEqual(values(actions, 'error'), [null]);
    assert.deepEqual(values(actions, 'data'), [undefined, null]);
    assert.deepEqual(values(actions, 'loading-state'), [true, false]);
});

test('batch results follow request ids, not server response order', async t => {
    const actions = await run(t, async () => ({ ok: true, json: async () => [reply('names', ['b']), reply('page', { items: [] })] }), batch);
    assert.deepEqual(values(actions, 'data'), [undefined, [{ items: [] }, ['b']]]);
    assert.deepEqual(values(actions, 'error'), [null]);
});

for (const [name, implementation, params, message] of [
    ['HTTP failure', async () => ({ ok: false, status: 503 }), {}, /HTTP 503/],
    ['network rejection', async () => { throw new Error('offline'); }, {}, /offline/],
    ['JSON parsing failure', async () => ({ ok: true, json: async () => { throw new SyntaxError('bad JSON'); } }), {}, /bad JSON/],
    ['RPC error', async () => ({ ok: true, json: async () => [{ jsonrpc: '2.0', id: 'page', error: { code: -1, message: 'denied' } }, reply('names', [])] }), batch, /denied/],
    ['missing batch response', async () => ({ ok: true, json: async () => [reply('page', [])] }), batch, /count/],
    ['unknown response id', async () => ({ ok: true, json: async () => [reply('other', []), reply('names', [])] }), batch, /Missing/],
    ['duplicate response id', async () => ({ ok: true, json: async () => [reply('page', []), reply('page', [])] }), batch, /duplicate/],
    ['malformed envelope', async () => ({ ok: true, json: async () => [{ jsonrpc: '2.0', id: 'page' }, reply('names', [])] }), batch, /envelope/],
    ['empty batch request', async () => { throw new Error('must not fetch'); }, [], /unique ids/],
]) {
    test(name, async t => {
        const actions = await run(t, implementation, params);
        assert.match(values(actions, 'error')[1].message, message);
        assert.deepEqual(values(actions, 'loading-state'), [true, false]);
        assert.deepEqual(values(actions, 'data'), [undefined]);
    });
}

test('header errors are dispatched and finish loading', async () => {
    const actions = [];
    requestJsonRpc(method, {}, action => actions.push(action), () => { throw new Error('headers failed'); });
    await setImmediate();
    assert.equal(values(actions, 'error')[1].message, 'headers failed');
    assert.deepEqual(values(actions, 'loading-state'), [true, false]);
});

test('cleanup aborts the actual fetch and suppresses late responses', async t => {
    let signal;
    let resolve;
    mockFetch(t, (_url, options) => {
        signal = options.signal;
        return new Promise(done => { resolve = done; });
    });
    const actions = [];
    const cleanup = requestJsonRpc(method, {}, action => actions.push(action));
    await setImmediate();
    cleanup();
    assert.equal(signal.aborted, true);
    const count = actions.length;
    resolve({ ok: true, json: async () => reply(0, 'stale') });
    await setImmediate();
    assert.equal(actions.length, count);
});

test('cleanup suppresses abort rejection without an unhandled rejection', async t => {
    mockFetch(t, (_url, { signal }) => new Promise((_resolve, reject) => {
        signal.addEventListener('abort', () => reject(new DOMException('Aborted', 'AbortError')));
    }));
    const actions = [];
    const cleanup = requestJsonRpc(method, {}, action => actions.push(action));
    await setImmediate();
    cleanup();
    await setImmediate();
    assert.deepEqual(values(actions, 'error'), [null]);
    assert.deepEqual(values(actions, 'loading-state'), [true]);
});

test('immediate cleanup prevents an obsolete request from starting', async t => {
    let calls = 0;
    mockFetch(t, async () => { calls++; });
    const cleanup = requestJsonRpc(method, {}, () => {});
    cleanup();
    await setImmediate();
    assert.equal(calls, 0);
});

import { readFileSync } from 'node:fs';
import http from 'node:http';
import { JSONRPCServer } from 'json-rpc-2.0';

const fixture = name => JSON.parse(readFileSync(new URL(`../mocks/${name}.json`, import.meta.url), 'utf8'));
const execution = fixture('execution');
const executions = fixture('executions');
const lastExecutions = fixture('last-executions');
const MAX_BODY = 1024 * 1024;

const reply = (res, status, body) => {
  res.writeHead(status, { 'Content-Type': 'application/json', 'Cache-Control': 'no-store' });
  res.end(body === undefined ? undefined : JSON.stringify(body));
};

export function createDevMiddleware({ backend } = {}) {
  let upstream;
  if (backend) {
    upstream = new URL(backend);
    if (upstream.protocol !== 'http:' || !['localhost', '127.0.0.1', '[::1]'].includes(upstream.hostname) ||
        upstream.username || upstream.password || upstream.pathname !== '/' || upstream.search || upstream.hash) {
      throw new Error('BATCH_BACKEND must be an HTTP localhost origin');
    }
  }
  const server = new JSONRPCServer();
  const list = (data, params = {}) => {
    const filtered = params.batch ? data.items.filter(item => item.name === params.batch) : data.items;
    const limit = Number.isInteger(params.pageSize) && params.pageSize > 0 ? Math.min(params.pageSize, 1000) : 10;
    const page = Number.isInteger(params.page) && params.page >= 0 ? params.page : 0;
    const offset = page * limit;
    return { ...data, total: filtered.length, items: filtered.slice(offset, offset + limit) };
  };
  server.addMethod('yupiik-batch-execution', ({ id } = {}) => {
    const item = executions.items.find(item => item.id === id);
    if (id && !item && id !== execution.id) {
      throw new Error('Execution not found');
    }
    return { ...execution, ...(item || {}) };
  });
  server.addMethod('yupiik-batch-executions', params => list(executions, params));
  server.addMethod('yupiik-batch-last-executions', () => lastExecutions);

  return async (req, res, next) => {
    if (req.url?.split('?')[0] !== '/jsonrpc') {
      next();
      return;
    }
    const host = req.headers.host;
    let origin;
    try {
      const url = new URL(`http://${host}`);
      if (!['localhost', '127.0.0.1', '[::1]'].includes(url.hostname) || url.username || url.password) {
        reply(res, 403, { error: 'Forbidden host' });
        return;
      }
      origin = url.origin;
    } catch {
      reply(res, 403, { error: 'Forbidden host' });
      return;
    }
    if (req.headers.origin && req.headers.origin !== origin) {
      reply(res, 403, { error: 'Forbidden origin' });
      return;
    }
    if (req.method !== 'POST') {
      res.setHeader('Allow', 'POST');
      reply(res, 405, { error: 'POST required' });
      return;
    }
    if (req.headers['content-type']?.split(';')[0].trim().toLowerCase() !== 'application/json') {
      reply(res, 415, { error: 'application/json required' });
      return;
    }
    try {
      let size = 0;
      const chunks = [];
      for await (const chunk of req) {
        size += chunk.length;
        if (size > MAX_BODY) {
          reply(res, 413, { error: 'Request too large' });
          return;
        }
        chunks.push(chunk);
      }
      const body = Buffer.concat(chunks);
      if (upstream) {
        const headers = { ...req.headers };
        for (const name of ['host', 'origin', 'connection', 'transfer-encoding', 'content-length']) {
          delete headers[name];
        }
        const proxy = http.request(new URL('/jsonrpc', upstream), {
          method: 'POST',
          headers: { ...headers, 'Content-Type': 'application/json', 'Content-Length': body.length },
          timeout: 10_000,
        }, response => {
          res.writeHead(response.statusCode, {
            'Content-Type': response.headers['content-type'] || 'application/json',
            'Cache-Control': 'no-store',
            ...(response.headers['content-encoding'] ? { 'Content-Encoding': response.headers['content-encoding'] } : {}),
          });
          response.on('error', () => res.destroy());
          response.pipe(res);
        });
        proxy.on('timeout', () => proxy.destroy(new Error('Backend timed out')));
        proxy.on('error', () => {
          if (!res.headersSent) {
            reply(res, 502, { error: 'Backend unavailable' });
          } else {
            res.destroy();
          }
        });
        res.on('close', () => proxy.destroy());
        proxy.end(body);
        return;
      }
      let request;
      try {
        request = JSON.parse(body.toString('utf8'));
      } catch {
        reply(res, 200, { jsonrpc: '2.0', id: null, error: { code: -32700, message: 'Parse error' } });
        return;
      }
      if (Array.isArray(request) && request.length > 100) {
        reply(res, 413, { error: 'Batch too large' });
        return;
      }
      const response = await server.receive(request);
      reply(res, response == null ? 204 : 200, response ?? undefined);
    } catch {
      if (!res.headersSent && !res.destroyed) {
        reply(res, 400, { error: 'Invalid request' });
      }
    }
  };
}

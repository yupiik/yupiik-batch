// Framework-independent request lifecycle, also exercised directly by node:test.
let idCounter = 0;

function responseData(request, response) {
    const batch = Array.isArray(request);
    const requests = batch ? request : [request];
    const responses = batch ? response : [response];
    if (!Array.isArray(responses) || responses.length !== requests.length) {
        throw new Error('Invalid JSON-RPC response count.');
    }
    const byId = new Map();
    for (const item of responses) {
        if (!item || item.jsonrpc !== '2.0' || byId.has(item.id)) {
            throw new Error('Invalid JSON-RPC response envelope or duplicate id.');
        }
        byId.set(item.id, item);
    }
    const results = requests.map(({ id }) => {
        const item = byId.get(id);
        if (!item) {
            throw new Error(`Missing JSON-RPC response for id ${id}.`);
        }
        const hasResult = Object.prototype.hasOwnProperty.call(item, 'result');
        const hasError = Object.prototype.hasOwnProperty.call(item, 'error');
        if (hasError === hasResult) {
            throw new Error('Invalid JSON-RPC result/error envelope.');
        }
        if (hasError) {
            throw new Error(item.error?.message || `JSON-RPC error ${item.error?.code ?? 'unknown'}.`);
        }
        return item.result;
    });
    return batch ? results : results[0];
}

export function requestJsonRpc(method, params, realDispatch, headers = () => ({})) {
    let active = true;
    const controller = new AbortController();
    const dispatch = (suffix, value) => {
        if (active) {
            realDispatch({ type: `${method}-${suffix}`, value });
        }
    };
    dispatch('error', null);
    dispatch('data', undefined);
    dispatch('loading-state', true);

    // Start inside the promise chain so header, serialization and synchronous fetch
    // failures follow the same error/loading lifecycle as network failures.
    Promise.resolve().then(() => {
        if (!active) {
            return;
        }
        const request = Array.isArray(params) ? params : {
            jsonrpc: '2.0', id: idCounter++, method, params,
        };
        if (Array.isArray(request) && (request.length === 0 ||
            new Set(request.map(item => item.id)).size !== request.length ||
            request.some(item => item.id === undefined || item.id === null))) {
            throw new Error('JSON-RPC batch requests require unique ids.');
        }
        return fetch(`/jsonrpc?m=${encodeURIComponent(method)}`, {
            method: 'POST',
            headers: {
                Accept: 'application/json',
                'Content-Type': 'application/json',
                ...headers(),
            },
            body: JSON.stringify(request),
            signal: controller.signal,
        }).then(response => {
            if (!response.ok) {
                throw new Error(`Invalid response status: HTTP ${response.status}.`);
            }
            return response.json();
        }).then(response => dispatch('data', responseData(request, response)));
    }).catch(error => {
        dispatch('error', { message: error?.message || String(error) });
    }).finally(() => {
        dispatch('loading-state', false);
        active = false;
    });

    return () => {
        active = false;
        controller.abort();
    };
}

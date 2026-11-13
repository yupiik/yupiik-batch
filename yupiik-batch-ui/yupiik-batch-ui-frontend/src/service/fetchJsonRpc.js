import extensions from '../extensions/extensions';
import { requestJsonRpc } from './requestJsonRpc.mjs';

// Keep the extension-facing signature and effect cleanup contract unchanged.
export function fetchJsonRpc(method, params, dispatch) {
    return requestJsonRpc(method, params, dispatch, () => extensions.fetchJsonRpcHeaders?.() || {});
}

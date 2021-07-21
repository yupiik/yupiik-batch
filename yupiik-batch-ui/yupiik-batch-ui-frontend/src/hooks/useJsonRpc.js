import { useEffect, useState } from "react";

// todo: move to useReducer which would enable extensions to access more easily data too
let idCounter = 0;
export function useJsonRpc(method, params) {
    const [loading, setLoading] = useState(true);
    const [data, setData] = useState(null);
    const [error, setError] = useState(null);

    useEffect(() => {
        const id = idCounter++;
        setLoading(true);
        fetch(
            `/jsonrpc?m=${encodeURIComponent(method)}`,
            {
                method: 'POST',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    jsonrpc: '2.0',
                    id,
                    method,
                    params,
                }),
            })
            .then(res => {
                if (res.status !== 200) {
                    setError({
                        message: `Invalid response status: HTTP ${res.status}.`,
                    });
                } else {
                    return res.json();
                }
            })
            .then(json => {
                if (!json) {
                    return;
                }
                if (json.error) {
                    setError({
                        message: `Invalid response: ${json.error.message || ''}.`,
                    });
                } else {
                    setData(json.result);
                }
            })
            .finally(() => setLoading(false));
        return () => { };
    }, [method, params]);

    return { loading, data, error };
};
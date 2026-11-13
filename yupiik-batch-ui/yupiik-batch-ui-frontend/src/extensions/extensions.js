import React from 'react';
import * as ReactDOMClient from 'react-dom/client';
import * as router from 'react-router-dom';
import * as antdIcons from '@ant-design/icons';
import * as antd from 'antd';
import { fetchJsonRpc } from '../service/fetchJsonRpc';

/*
window.yupiikBatchExtensions = {
    routes: function () {
        return [...]; // routes as below
    },
    commentWrapper: function (comment) {
        return comment; // or any react component
    },
    tableColumnsWrapper: function ({ columns, method }) {
        return columns;
    }
};
*/

// ensure there is a default for all extensions
const exts = {
    routes: () => [],
    commentWrapper: c => c,
    routeDecorator: c => c,
    // tableColumnWrapper, not set is a better default
    ...(window.yupiikBatchExtensions || {}),
};

// Hooks keep their existing signatures; the second argument is this registry.
// React is React 19; ReactDOMClient exposes createRoot (no legacy render).
// router is React Router 7: use hooks, Routes/Navigate and explicit /* paths
// for descendant matching, rather than withRouter, Switch or exact.
// antd/antdIcons expose the installed Ant Design 6 APIs (items, no PageHeader).
// bootstrap receives { ReactDOMClient, React, App, antd } and owns its root.
// fetchJsonRpc(method, params, dispatch) returns an abort/cleanup function.
const registry = { React, ReactDOMClient, router, fetchJsonRpc, antd, antdIcons };
const extensions = {
    ...exts,
    routes: () => exts.routes(registry),
    commentWrapper: c => exts.commentWrapper(c, registry),
    routeDecorator: r => exts.routeDecorator(r, registry),
};
if (exts.tableColumnsWrapper) {
    extensions.tableColumnsWrapper = data => exts.tableColumnsWrapper(data, registry);
}

export default extensions;

import React from 'react';
import * as router from 'react-router-dom';
import * as antdIcons from '@ant-design/icons';
import * as antd from 'antd';
import { useJsonRpc } from '../hooks/useJsonRpc';

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

// inject react, router, antd and antdIcons in all extensions
const registry = { React, router, useJsonRpc, antd, antdIcons };
const extensions = {
    routes: () => exts.routes(registry),
    commentWrapper: c => exts.commentWrapper(c, registry),
    routeDecorator: r => exts.routeDecorator(r, registry),
};
if (exts.tableColumnsWrapper) {
    extensions.tableColumnsWrapper = data => exts.tableColumnsWrapper(data, registry);
}

export default extensions;

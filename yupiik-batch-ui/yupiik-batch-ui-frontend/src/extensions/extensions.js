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
};
*/

// ensure there is a default for all extensions
const exts = {
    routes: () => [],
    commentWrapper: c => c,
    ...(window.yupiikBatchExtensions || {}),
};

// inject react, router, antd and antdIcons in all extensions
const registry = { React, router, useJsonRpc, antd, antdIcons };
const extensions = {
    routes: () => exts.routes(registry),
    commentWrapper: c => exts.commentWrapper(c, registry),
};

export default extensions;

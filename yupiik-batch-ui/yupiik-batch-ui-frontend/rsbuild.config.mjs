import { defineConfig } from '@rsbuild/core';
import { pluginReact } from '@rsbuild/plugin-react';
import { createDevMiddleware } from './dev/jsonrpc.mjs';

export default defineConfig({
  plugins: [pluginReact()],
  source: { entry: { main: './src/index.js' } },
  html: {
    template: './public/index.html',
    inject: 'head',
    scriptLoading: 'defer',
  },
  output: {
    distPath: { root: 'target/classes/META-INF/resources' },
    filename: { html: 'index.html' },
    assetPrefix: '/',
    cleanDistPath: true,
    manifest: {
      filename: 'asset-manifest.json',
      generate: ({ files, manifestData }) => ({
        files: Object.fromEntries(files.map(file => [file.name, file.path])),
        entrypoints: [
          ...(manifestData.entries.main?.initial?.css || []),
          ...(manifestData.entries.main?.initial?.js || []),
        ].map(path => path.replace(/^\//, '')),
      }),
    },
  },
  server: {
    host: 'localhost',
    port: 3000,
    strictPort: true,
    allowedHosts: ['localhost'],
    setup: ({ server, action }) => {
      if (action === 'dev') {
        server.middlewares.use(createDevMiddleware({ backend: process.env.BATCH_BACKEND }));
      }
    },
  },
});

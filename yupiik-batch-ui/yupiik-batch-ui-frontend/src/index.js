import React from 'react';
import * as ReactDOMClient from 'react-dom/client';
import * as antd from 'antd';
import App from './App';
import reportWebVitals from './reportWebVitals';
import 'antd/dist/reset.css';
import './index.css';
import extensions from './extensions/extensions';

const bootstrap = extensions.bootstrap || (({ ReactDOMClient, React, App }) => {
  ReactDOMClient.createRoot(document.getElementById('root')).render(
    <React.StrictMode>
      <App />
    </React.StrictMode>
  );
  reportWebVitals();
});
bootstrap({ ReactDOMClient, React, App, antd });

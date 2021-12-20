import React from 'react';
import ReactDOM from 'react-dom';
import App from './App';
import reportWebVitals from './reportWebVitals';

import 'antd/dist/antd.min.css'
import * as antd from 'antd';

import './index.css';
import extensions from "./extensions/extensions";



const bootstrap = (extensions.bootstrap || (function ({ ReactDOM, React , App, antd }) {
  ReactDOM.render(
    <React.StrictMode>
      <App />
    </React.StrictMode>,
    document.getElementById('root')
  );
  // If you want to start measuring performance in your app, pass a function
  // to log results (for example: reportWebVitals(console.log))
  // or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
  reportWebVitals();
}));
bootstrap({ ReactDOM, React , App, antd });

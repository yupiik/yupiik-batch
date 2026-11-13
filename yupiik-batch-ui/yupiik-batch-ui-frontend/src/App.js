import React, { useReducer, useState } from 'react';
import { Layout, Menu } from 'antd';
import { BrowserRouter as Router, Link, Navigate, Route, Routes } from 'react-router-dom';
import logo from './logo.svg';
import './App.css';

import routes from './routes';
import reducer from './reducers';

function SwitchRoutes({ component, includeRedirect, filter }) {
  return (
    <Routes>
      {routes.filter(route => filter == null || filter(route)).map((route, key) => (
        <Route path={route.path} key={key} element={component(route)} />
      ))}
      <Route path="*" element={includeRedirect ? <Navigate to="/" replace /> : null} />
    </Routes>
  );
}

function SideMenuItems({ selectedKey }) {
  return (
    <Menu theme="dark" selectedKeys={selectedKey ? [selectedKey] : []} mode="inline"
          items={routes.filter(route => route.sider?.menu).map(route => ({
            ...route.sider.menu.attributes,
            label: route.sider.menu.content,
          }))} />
  );
}

function SideMenu() {
  const [collapsed, setCollapsed] = useState(false);
  return (
    <Layout.Sider collapsible breakpoint="lg" collapsedWidth={64} collapsed={collapsed} onCollapse={setCollapsed} onBreakpoint={setCollapsed}>
      <div className="logo">
        <Link to="/"><img src={logo} alt="Yupiik" width={32} height={32} /></Link>
        {!collapsed && <Link to="/">Yupiik Batch UI</Link>}
      </div>
      <SwitchRoutes filter={route => route.sider} component={route => (<SideMenuItems selectedKey={route.sider.selectedKey} />)} />
    </Layout.Sider>
  );
}

function Content() {
  const [state, dispatch] = useReducer(reducer, {});
  return (
    <Layout.Content>
      <SwitchRoutes filter={route => route.component.Breadcrumb}
                    component={route => (<route.component.Breadcrumb />)} />
      <div className="site-layout-background">
        <SwitchRoutes component={route => (<route.component state={state} dispatch={dispatch} />)}
                      includeRedirect={true} />
      </div>
    </Layout.Content>
  );
}

export default function App() {
  return (
    <Layout className="layout">
      <Router>
        <SideMenu />
        <Layout className="site-layout">
          <Content />
          <Layout.Footer>Yupiik ©{new Date().getFullYear()}</Layout.Footer>
        </Layout>
      </Router>
    </Layout>
  );
}

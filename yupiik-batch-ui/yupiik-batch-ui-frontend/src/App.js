import React, { useReducer, useState } from 'react';
import { Layout, Menu } from 'antd';
import { Redirect, Route, Switch } from 'react-router';
import { BrowserRouter as Router, Link } from 'react-router-dom';
import { ReactComponent as Logo } from './logo.svg';
import './App.css';

import routes from './routes';
import reducer from "./reducers";

function SwitchRoutes({ component, includeRedirect, filter }) {
  return (
    <Switch>
      {routes.filter(route => filter == null || filter(route)).map((route, key) => (
        <Route path={route.path} key={key} exact={route.exact}>
          {component(route)}
        </Route>
      ))}
      {includeRedirect && <Redirect from="*" to="/" />}
    </Switch>
  );
}


function SideMenuItems(props) {
  return (
    <Menu theme="dark" selectedKeys={[props.selectedKey]} mode="inline">
      {routes.filter(route => route.sider.menu).map(route => (
        <Menu.Item {...route.sider.menu.attributes}>{route.sider.menu.content}</Menu.Item>
      ))}
    </Menu>
  );
}

function SideMenu() {
  const [collapsed, setCollapsed] = useState(false);
  return (
    <Layout.Sider collapsible collapsed={collapsed} onCollapse={newValue => setCollapsed(newValue)}>
      <div className="logo">
        <Link to="/"><Logo /></Link>
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
                    component={route => (<route.component.Breadcrumb/>)}/>
      <div className="site-layout-background">
        <SwitchRoutes component={route => (<route.component state={state} dispatch={dispatch}/>)}
                      includeRedirect={true}/>
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
          <Layout.Footer>Yupiik ©2021</Layout.Footer>
        </Layout>
      </Router>
    </Layout>
  );
}

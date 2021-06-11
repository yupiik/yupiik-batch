import { HistoryOutlined, HomeOutlined, RadarChartOutlined } from '@ant-design/icons';
import { Link } from 'react-router-dom';
import Execution from './components/executions/Execution';
import { ExecutionHistory, LastExecutions } from './components/executions/Executions';
import Home from './components/home/Home';
import extensions from './extensions/extensions';


const routes = [
    {
        path: '/',
        exact: true,
        component: Home,
        sider: {
            selectedKey: 'home',
            menu: {
                attributes: {
                    icon: <HomeOutlined />,
                },
                content: <Link to="/">Home</Link>,
            },
        },
    },
    {
        path: '/executions',
        component: ExecutionHistory,
        sider: {
            selectedKey: 'executions',
            menu: {
                attributes: {
                    icon: <HistoryOutlined />,
                },
                content: <Link to="/executions">Execution history</Link>,
            },
        },
    },
    {
        path: '/last-executions',
        component: LastExecutions,
        sider: {
            selectedKey: 'last-executions',
            menu: {
                attributes: {
                    icon: <RadarChartOutlined />,
                },
                content: <Link to="/last-executions">Last Executions</Link>,
            },
        },
    },
    {
        path: '/execution/:id',
        component: Execution,
        sider: {},
    },
    ...(extensions.routes)(),
].map(it => ({ // ensure menu.key = sider.selectedKey if not set
    ...it,
    sider: it.sider ?
        {
            ...it.sider,
            menu: it.sider.menu ?
                {
                    ...it.sider.menu,
                    attributes: {
                        key: it.sider.selectedKey,
                        ...(it.sider.menu.attributes || {}),
                    },
                } :
                it.sider.menu,
        } :
        it.sider,
}));

export default routes;

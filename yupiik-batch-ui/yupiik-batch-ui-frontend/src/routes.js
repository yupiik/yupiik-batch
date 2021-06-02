import { Link } from 'react-router-dom';
import { HistoryOutlined, HomeOutlined } from '@ant-design/icons';
import Home from './components/home/Home';
import Executions from './components/executions/Executions';
import Execution from './components/executions/Execution';
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
        component: Executions,
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

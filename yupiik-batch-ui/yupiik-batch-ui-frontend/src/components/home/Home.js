import { HomeOutlined } from '@ant-design/icons';
import { Breadcrumb } from 'antd';

function HomeBreadcrumb() {
    return (
        <Breadcrumb>
            <Breadcrumb.Item><HomeOutlined /></Breadcrumb.Item>
        </Breadcrumb>
    );
}

function Home() {
    return (
        <div>Welcome to Yupiik batch UI. Please select a link from the menu.</div>
    );
}

Home.Breadcrumb = HomeBreadcrumb;

export default Home;

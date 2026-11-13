import { HomeOutlined } from '@ant-design/icons';
import { Breadcrumb } from 'antd';

function HomeBreadcrumb() {
    return (
        <Breadcrumb items={[{ title: <HomeOutlined /> }]} />
    );
}

function Home() {
    return (
        <div>Welcome to Yupiik batch UI. Please select a link from the menu.</div>
    );
}

Home.Breadcrumb = HomeBreadcrumb;

export default Home;

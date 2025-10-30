import React from 'react';
import { BrowserRouter as Router, Routes, Route, Link, useLocation } from 'react-router-dom';
import { Layout, Menu, ConfigProvider, theme, Typography, Breadcrumb } from 'antd';
import { HomeOutlined, MessageOutlined, TeamOutlined, BookOutlined, UserOutlined } from '@ant-design/icons';
import ruRU from 'antd/locale/ru_RU';
import HomePage from './features/home/HomePage';
import UserList from './features/users/UserList';
import TeacherList from './features/teachers/TeacherList';
import SubjectList from './features/subjects/SubjectList';
import ReviewList from './features/reviews/ReviewList';
import { RatingProvider } from './context/RatingContext'; // Импортируем провайдер
import './index.css';

const { Header, Content, Footer } = Layout;
const { Title } = Typography;

const UsersPage = () => <UserList />;
const TeachersPage = () => <TeacherList />;
const SubjectsPage = () => <SubjectList />;
const AllReviewsPage = () => <ReviewList title="Все отзывы" />;
const UserReviewsPage = () => <ReviewList mode="user" />;
const TeacherReviewsPage = () => <ReviewList mode="teacher" />;

const menuItems = [
  { key: '/', icon: <HomeOutlined />, label: <Link to="/">Главная</Link> },
  { key: '/reviews', icon: <MessageOutlined />, label: <Link to="/reviews">Отзывы</Link> },
  { key: '/teachers', icon: <TeamOutlined />, label: <Link to="/teachers">Преподаватели</Link> },
  { key: '/subjects', icon: <BookOutlined />, label: <Link to="/subjects">Предметы</Link> },
  { key: '/users', icon: <UserOutlined />, label: <Link to="/users">Пользователи</Link> },
];

const breadcrumbNameMap = {
  '/reviews': 'Отзывы',
  '/teachers': 'Преподаватели',
  '/teachers/:teacherId/reviews': 'Отзывы о преподавателе',
  '/subjects': 'Предметы',
  '/users': 'Пользователи',
  '/users/:userId/reviews': 'Отзывы пользователя',
};

const AppBreadcrumb = () => {
  const location = useLocation();
  const pathSnippets = location.pathname.split('/').filter(i => i);

  const getBreadcrumbName = (url, index) => {
    const currentPath = `/${pathSnippets.slice(0, index + 1).join('/')}`;
    const foundKey = Object.keys(breadcrumbNameMap).find(key => {
      const keyParts = key.split('/').filter(p => p);
      const currentPathParts = currentPath.split('/').filter(p => p);
      if (keyParts.length !== currentPathParts.length) return false;
      return keyParts.every((part, i) => part.startsWith(':') || part === currentPathParts[i]);
    });
    return foundKey ? breadcrumbNameMap[foundKey] : pathSnippets[index];
  }

  const extraBreadcrumbItems = pathSnippets.map((_, index) => {
    const url = `/${pathSnippets.slice(0, index + 1).join('/')}`;
    const name = getBreadcrumbName(url, index);
    const isLast = index === pathSnippets.length - 1;
    const showCrumb = Object.values(breadcrumbNameMap).includes(name);

    return showCrumb ? (
        <Breadcrumb.Item key={url}>
          {isLast ? <span>{name}</span> : <Link to={url}>{name}</Link>}
        </Breadcrumb.Item>
    ) : null;
  }).filter(item => item !== null);

  const breadcrumbItems = [
    <Breadcrumb.Item key="home">
      <Link to="/"><HomeOutlined /></Link>
    </Breadcrumb.Item>,
  ].concat(extraBreadcrumbItems);

  if (location.pathname === '/' || extraBreadcrumbItems.length === 0) return null;

  return (
      <Breadcrumb style={{ margin: '16px 0' }}>
        {breadcrumbItems}
      </Breadcrumb>
  );
};

function App() {
  const { token } = theme.useToken();
  const location = useLocation();

  const getSelectedKey = () => {
    const path = location.pathname;
    if (path.startsWith('/reviews')) return '/reviews';
    if (path.startsWith('/teachers')) return '/teachers';
    if (path.startsWith('/subjects')) return '/subjects';
    if (path.startsWith('/users')) return '/users';
    return '/';
  }

  return (
      <ConfigProvider
          locale={ruRU}
          theme={{
            token: { colorPrimary: '#1677ff' },
          }}
      >
        {}
        <RatingProvider>
          <Layout style={{ minHeight: '100vh' }}>
            <Header style={{ display: 'flex', alignItems: 'center', padding: '0 24px', backgroundColor: token.colorPrimary }}>
              <Title level={3} style={{ color: 'white', margin: 0, marginRight: 'auto' }}>Spring Reviewer</Title>
              <Menu
                  theme="dark"
                  mode="horizontal"
                  selectedKeys={[getSelectedKey()]}
                  items={menuItems}
                  style={{ flex: 1, minWidth: 0, justifyContent: 'flex-end', backgroundColor: token.colorPrimary }}
              />
            </Header>
            <Content style={{ padding: '0 24px' }}>
              <AppBreadcrumb />
              <div style={{ background: '#fff', padding: 24, minHeight: 280, borderRadius: '8px' }}>
                <Routes>
                  <Route path="/" element={<HomePage />} />
                  <Route path="/users" element={<UsersPage />} />
                  <Route path="/users/:userId/reviews" element={<UserReviewsPage />} />
                  <Route path="/teachers" element={<TeachersPage />} />
                  <Route path="/teachers/:teacherId/reviews" element={<TeacherReviewsPage />} />
                  <Route path="/subjects" element={<SubjectsPage />} />
                  <Route path="/reviews" element={<AllReviewsPage />} />
                  <Route path="*" element={<Title level={3}>404 - Страница не найдена</Title>} />
                </Routes>
              </div>
            </Content>
            <Footer style={{ textAlign: 'center', backgroundColor: '#f0f2f5' }}>
              Spring Reviewer UI ©{new Date().getFullYear()} Created with Ant Design & React Router
            </Footer>
          </Layout>
        </RatingProvider>
      </ConfigProvider>
  );
}

const AppWrapper = () => (
    <Router>
      <App />
    </Router>
);

export default AppWrapper;
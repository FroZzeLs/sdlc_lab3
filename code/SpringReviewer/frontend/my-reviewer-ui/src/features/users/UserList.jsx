import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { List, Button, Typography, Spin, Popconfirm, message, Avatar, Tooltip } from 'antd';
import { UserOutlined, PlusOutlined, EditOutlined, DeleteOutlined, EyeOutlined } from '@ant-design/icons';
import * as api from '../../api';
import UserForm from './UserForm';

const { Title, Text } = Typography;

const UserList = () => {
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(false);
    const [isModalVisible, setIsModalVisible] = useState(false);
    const [editingUser, setEditingUser] = useState(null);
    const [messageApi, contextHolder] = message.useMessage();

    const fetchUsers = useCallback(async () => {
        setLoading(true);
        try {
            const response = await api.getUsers();
            setUsers(response.data);
        } catch (error) {
            api.handleApiError(error, messageApi);
        } finally {
            setLoading(false);
        }
    }, [messageApi]);

    useEffect(() => {
        fetchUsers();
    }, [fetchUsers]);

    const handleAdd = () => {
        setEditingUser(null);
        setIsModalVisible(true);
    };

    const handleEdit = (user) => {
        setEditingUser(user);
        setIsModalVisible(true);
    };

    const handleDelete = async (id) => {
        try {
            await api.deleteUser(id);
            messageApi.success('Пользователь успешно удален');
            fetchUsers();
        } catch (error) {
            api.handleApiError(error, messageApi);
        }
    };

    const handleCreate = async (values) => {
        try {
            await api.createUser(values);
            messageApi.success('Пользователь успешно создан');
            setIsModalVisible(false);
            fetchUsers();
        } catch (error) {
            api.handleApiError(error, messageApi);
        }
    };

    const handleUpdate = async (id, values) => {
        try {
            await api.updateUser(id, values);
            messageApi.success('Пользователь успешно обновлен');
            setIsModalVisible(false);
            setEditingUser(null);
            fetchUsers();
        } catch (error) {
            api.handleApiError(error, messageApi);
        }
    };

    const handleCancel = () => {
        setIsModalVisible(false);
        setEditingUser(null);
    };

    return (
        <div className="list-container">
            {contextHolder}
            <Title level={4} className="section-title">Пользователи</Title>
            <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={handleAdd}
                style={{ marginBottom: 16 }}
            >
                Добавить пользователя
            </Button>
            <Spin spinning={loading}>
                <List
                    itemLayout="horizontal"
                    dataSource={users}
                    renderItem={(user) => (
                        <List.Item
                            className="list-item-margin"
                            actions={[
                                <Tooltip title="Посмотреть отзывы пользователя">
                                    <Link to={`/users/${user.id}/reviews`}>
                                        <Button type="link" icon={<EyeOutlined />} />
                                    </Link>
                                </Tooltip>,
                                <Tooltip title="Редактировать пользователя">
                                    <Button type="link" icon={<EditOutlined />} onClick={() => handleEdit(user)} />
                                </Tooltip>,
                                <Popconfirm
                                    title="Удалить пользователя?"
                                    description={`Вы уверены, что хотите удалить ${user.username}?`}
                                    onConfirm={() => handleDelete(user.id)}
                                    okText="Да, удалить"
                                    cancelText="Отмена"
                                >
                                    <Tooltip title="Удалить пользователя">
                                        <Button type="link" danger icon={<DeleteOutlined />} />
                                    </Tooltip>
                                </Popconfirm>,
                            ]}
                        >
                            <List.Item.Meta
                                avatar={<Avatar icon={<UserOutlined />} />}
                                title={<Text strong>{user.username}</Text>}
                                description={`ID: ${user.id}`}
                            />
                        </List.Item>
                    )}
                    locale={{ emptyText: 'Нет пользователей' }}
                />
            </Spin>
            <UserForm
                visible={isModalVisible}
                onCreate={handleCreate}
                onUpdate={handleUpdate}
                onCancel={handleCancel}
                editingUser={editingUser}
            />
        </div>
    );
};

export default UserList;
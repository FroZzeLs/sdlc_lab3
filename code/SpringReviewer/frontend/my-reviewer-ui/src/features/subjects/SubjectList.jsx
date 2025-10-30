import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { List, Button, Typography, Spin, Popconfirm, message, Tag, Input, Space, Row, Col } from 'antd';
import { BookOutlined, PlusOutlined, EditOutlined, DeleteOutlined, SortAscendingOutlined, SortDescendingOutlined } from '@ant-design/icons';
import * as api from '../../api';
import SubjectForm from './SubjectForm';

const { Title, Text } = Typography;
const { Search } = Input;

const SubjectList = () => {
    const [allSubjects, setAllSubjects] = useState([]);
    const [filteredSubjects, setFilteredSubjects] = useState([]);
    const [loading, setLoading] = useState(false);
    const [isModalVisible, setIsModalVisible] = useState(false);
    const [editingSubject, setEditingSubject] = useState(null);
    const [searchText, setSearchText] = useState('');
    const [sortOrder, setSortOrder] = useState('asc');
    const [messageApi, contextHolder] = message.useMessage();

    const fetchSubjects = useCallback(async () => {
        setLoading(true);
        try {
            const response = await api.getSubjects();
            const sortedData = response.data.slice().sort((a, b) =>
                a.name.localeCompare(b.name)
            );
            setAllSubjects(sortedData);
            setFilteredSubjects(sortedData);
            setSearchText('');
            setSortOrder('asc');
        } catch (error) {
            api.handleApiError(error, messageApi);
        } finally {
            setLoading(false);
        }
    }, [messageApi]);

    useEffect(() => {
        fetchSubjects();
    }, [fetchSubjects]);

    useEffect(() => {
        const lowercasedFilter = searchText.toLowerCase();
        let filteredData = allSubjects.filter((item) =>
            item.name.toLowerCase().includes(lowercasedFilter)
        );

        filteredData.sort((a, b) => {
            const nameA = a.name || '';
            const nameB = b.name || '';
            if (sortOrder === 'asc') {
                return nameA.localeCompare(nameB);
            } else {
                return nameB.localeCompare(nameA);
            }
        });

        setFilteredSubjects(filteredData);
    }, [searchText, allSubjects, sortOrder]);

    const handleSearch = (value) => {
        setSearchText(value);
    };

    const handleSortToggle = () => {
        setSortOrder(prevOrder => (prevOrder === 'asc' ? 'desc' : 'asc'));
    };

    const handleAdd = () => {
        setEditingSubject(null);
        setIsModalVisible(true);
    };

    const handleEdit = (subject) => {
        setEditingSubject(subject);
        setIsModalVisible(true);
    };

    const handleDelete = async (id) => {
        try {
            await api.deleteSubject(id);
            messageApi.success('Предмет успешно удален');
            fetchSubjects();
        } catch (error) {
            api.handleApiError(error, messageApi);
        }
    };

    const handleCreate = async (values) => {
        try {
            await api.createSubject(values);
            messageApi.success('Предмет успешно создан');
            setIsModalVisible(false);
            fetchSubjects();
        } catch (error) {
            api.handleApiError(error, messageApi);
        }
    };

    const handleUpdate = async (id, values) => {
        try {
            await api.updateSubject(id, values);
            messageApi.success('Предмет успешно обновлен');
            setIsModalVisible(false);
            setEditingSubject(null);
            fetchSubjects();
        } catch (error) {
            api.handleApiError(error, messageApi);
        }
    };

    const handleCancel = () => {
        setIsModalVisible(false);
        setEditingSubject(null);
    };

    const SortIcon = sortOrder === 'asc' ? SortAscendingOutlined : SortDescendingOutlined;
    const sortButtonText = sortOrder === 'asc' ? 'Сорт. А-Я' : 'Сорт. Я-А';

    return (
        <div className="list-container">
            {contextHolder}
            <Title level={4} className="section-title">Предметы</Title>
            <Space direction="vertical" style={{ width: '100%', marginBottom: 16 }}>
                <Row gutter={16} wrap={false}>
                    <Col flex="auto">
                        <Search
                            placeholder="Поиск по названию предмета"
                            allowClear
                            enterButton="Найти"
                            value={searchText}
                            onChange={(e) => setSearchText(e.target.value)}
                            onSearch={handleSearch}
                        />
                    </Col>
                    <Col flex="none">
                        <Button icon={<SortIcon />} onClick={handleSortToggle}>
                            {sortButtonText}
                        </Button>
                    </Col>
                </Row>
                <Button
                    type="primary"
                    icon={<PlusOutlined />}
                    onClick={handleAdd}
                >
                    Добавить предмет
                </Button>
            </Space>
            <Spin spinning={loading}>
                <List
                    itemLayout="horizontal"
                    dataSource={filteredSubjects}
                    renderItem={(subject) => (
                        <List.Item
                            key={subject.id} // Добавлен ключ
                            className="list-item-margin"
                            actions={[
                                <Button type="link" icon={<EditOutlined />} onClick={() => handleEdit(subject)}>Редактировать</Button>,
                                <Popconfirm
                                    title="Удалить предмет?"
                                    description={`Вы уверены, что хотите удалить ${subject.name}?`}
                                    onConfirm={() => handleDelete(subject.id)}
                                    okText="Да, удалить"
                                    cancelText="Отмена"
                                >
                                    <Button type="link" danger icon={<DeleteOutlined />}>Удалить</Button>
                                </Popconfirm>,
                            ]}
                        >
                            <List.Item.Meta
                                avatar={<BookOutlined style={{ fontSize: '20px', color: '#1677ff' }}/>}
                                title={<Text strong>{subject.name}</Text>}
                                description={
                                    subject.teacherNames && subject.teacherNames.length > 0
                                        ? <>Преподаватели: {subject.teacherNames.map(name => <Tag key={name} color="blue">{name}</Tag>)}</>
                                        : 'Преподаватели не назначены'
                                }
                            />
                        </List.Item>
                    )}
                    locale={{ emptyText: searchText ? 'Предметы не найдены' : 'Нет предметов' }}
                />
            </Spin>
            <SubjectForm
                visible={isModalVisible}
                onCreate={handleCreate}
                onUpdate={handleUpdate}
                onCancel={handleCancel}
                editingSubject={editingSubject}
            />
        </div>
    );
};

export default SubjectList;
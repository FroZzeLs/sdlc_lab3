import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { List, Button, Typography, Spin, Popconfirm, message, Tag, Avatar, Tooltip, Input, Select, Space, Row, Col } from 'antd';
import { TeamOutlined, PlusOutlined, EditOutlined, DeleteOutlined, EyeOutlined, StarFilled, SortAscendingOutlined, SortDescendingOutlined } from '@ant-design/icons';
import * as api from '../../api';
import TeacherForm from './TeacherForm';
import { useRatings } from '../../context/RatingContext';
import { getRatingColor } from '../../utils/helpers';

const { Title, Text } = Typography;
const { Search } = Input;
const { Option } = Select;

const TeacherList = () => {
    const [allTeachers, setAllTeachers] = useState([]);
    const [filteredTeachers, setFilteredTeachers] = useState([]);
    const [loadingTeachers, setLoadingTeachers] = useState(false);
    const [isModalVisible, setIsModalVisible] = useState(false);
    const [editingTeacher, setEditingTeacher] = useState(null);
    const [messageApi, contextHolder] = message.useMessage();
    const [allSubjects, setAllSubjects] = useState([]);
    const [searchNameText, setSearchNameText] = useState('');
    const [selectedSubjectId, setSelectedSubjectId] = useState(null);
    const [sortOrder, setSortOrder] = useState('asc');

    const { ratings, isLoading: loadingRatings, fetchRatings } = useRatings();

    const getTeacherFullName = useCallback((teacher) => {
        if (!teacher) return '';
        return `${teacher.surname || ''} ${teacher.name || ''} ${teacher.patronym || ''}`.trim();
    }, []);

    const fetchAllSubjects = useCallback(async () => {
        try {
            const response = await api.getSubjects();
            setAllSubjects(response.data);
        } catch (error) {
            console.error("Failed to fetch all subjects for filter:", error);
            messageApi.error('Не удалось загрузить предметы для фильтра.');
        }
    }, [messageApi]);


    const fetchTeachers = useCallback(async () => {
        setLoadingTeachers(true);
        try {
            const response = await api.getTeachers();
            const initialSorted = response.data.slice().sort((a, b) =>
                getTeacherFullName(a).localeCompare(getTeacherFullName(b))
            );
            setAllTeachers(initialSorted);
            setFilteredTeachers(initialSorted);
            setSearchNameText('');
            setSelectedSubjectId(null);
            setSortOrder('asc');
        } catch (error) {
            api.handleApiError(error, messageApi);
        } finally {
            setLoadingTeachers(false);
        }
    }, [messageApi, getTeacherFullName]);


    useEffect(() => {
        fetchTeachers();
        fetchAllSubjects();
    }, [fetchTeachers, fetchAllSubjects]);

    useEffect(() => {
        const lowercasedNameFilter = searchNameText.toLowerCase();
        const subjectNameFilter = selectedSubjectId
            ? allSubjects.find(s => s.id === selectedSubjectId)?.name
            : null;

        let filteredData = allTeachers.filter(teacher => {
            const fullName = getTeacherFullName(teacher).toLowerCase();
            const nameMatch = !lowercasedNameFilter || fullName.includes(lowercasedNameFilter);
            const subjectMatch = !subjectNameFilter || (teacher.subjects && teacher.subjects.includes(subjectNameFilter));
            return nameMatch && subjectMatch;
        });

        filteredData.sort((a, b) => {
            const nameA = getTeacherFullName(a);
            const nameB = getTeacherFullName(b);
            if (sortOrder === 'asc') {
                return nameA.localeCompare(nameB);
            } else {
                return nameB.localeCompare(nameA);
            }
        });

        setFilteredTeachers(filteredData);

    }, [searchNameText, selectedSubjectId, allTeachers, allSubjects, getTeacherFullName, sortOrder]);


    const handleNameSearch = (value) => { setSearchNameText(value); };
    const handleSubjectFilterChange = (value) => { setSelectedSubjectId(value); };
    const handleAdd = () => { setIsModalVisible(true); setEditingTeacher(null); };
    const handleEdit = (teacher) => { setEditingTeacher(teacher); setIsModalVisible(true); };

    const handleSortToggle = () => {
        setSortOrder(prevOrder => (prevOrder === 'asc' ? 'desc' : 'asc'));
    };

    const handleDelete = async (id) => {
        try {
            await api.deleteTeacher(id);
            messageApi.success('Преподаватель успешно удален');
            fetchTeachers();
            fetchRatings();
        } catch (error) {
            api.handleApiError(error, messageApi);
        }
    };

    const updateTeacherSubjects = async (teacherId, newSubjectIds, currentSubjectNames) => {
        const currentSubjectIds = currentSubjectNames
            .map(name => allSubjects.find(s => s.name === name)?.id)
            .filter(id => id != null);

        const idsToAdd = newSubjectIds.filter(id => !currentSubjectIds.includes(id));
        const idsToRemove = currentSubjectIds.filter(id => !newSubjectIds.includes(id));

        const addPromises = idsToAdd.map(subjectId => api.assignSubjectToTeacher(teacherId, subjectId));
        const removePromises = idsToRemove.map(subjectId => api.removeSubjectFromTeacher(teacherId, subjectId));

        try {
            const results = await Promise.allSettled([...addPromises, ...removePromises]);
            const errors = results.filter(p => p.status === 'rejected');
            if (errors.length > 0) {
                console.error("Errors updating teacher subjects:", errors.map(e => e.reason));
                messageApi.error('Не удалось обновить все связи с предметами.');
            }
        } catch (error) {
            console.error("Unexpected error in updateTeacherSubjects:", error);
            messageApi.error('Критическая ошибка при обновлении предметов преподавателя.');
            throw error;
        }
    };

    const handleCreate = async (teacherData, selectedSubjectIds) => {
        let newTeacherId = null;
        try {
            const response = await api.createTeacher(teacherData);
            newTeacherId = response.data.id;
            messageApi.success('Преподаватель успешно создан');
            if (selectedSubjectIds && selectedSubjectIds.length > 0) {
                await updateTeacherSubjects(newTeacherId, selectedSubjectIds, []);
            }
            setIsModalVisible(false);
            fetchTeachers();
        } catch (error) { api.handleApiError(error, messageApi); }
    };

    const handleUpdate = async (id, teacherData, selectedSubjectIds) => {
        try {
            const updateResponse = await api.updateTeacher(id, teacherData);
            messageApi.success('Данные преподавателя успешно обновлены');
            const currentSubjectNames = updateResponse.data.subjects || [];
            await updateTeacherSubjects(id, selectedSubjectIds, currentSubjectNames);
            setIsModalVisible(false);
            setEditingTeacher(null);
            fetchTeachers();
        } catch (error) { api.handleApiError(error, messageApi); }
    };

    const handleCancel = () => { setIsModalVisible(false); setEditingTeacher(null); };

    const isLoading = loadingTeachers;
    const SortIcon = sortOrder === 'asc' ? SortAscendingOutlined : SortDescendingOutlined;
    const sortButtonText = sortOrder === 'asc' ? 'Сорт. А-Я' : 'Сорт. Я-А';

    return (
        <div className="list-container">
            {contextHolder}
            <Title level={4} className="section-title">Преподаватели</Title>
            <Space direction="vertical" style={{ width: '100%', marginBottom: 16 }}>
                <Row gutter={16}>
                    <Col xs={24} md={10}>
                        <Search
                            placeholder="Поиск по ФИО"
                            allowClear
                            value={searchNameText}
                            onChange={(e) => setSearchNameText(e.target.value)}
                            onSearch={handleNameSearch}
                            style={{ width: '100%' }}
                        />
                    </Col>
                    <Col xs={24} md={10}>
                        <Select
                            showSearch
                            allowClear
                            placeholder="Фильтр по предмету"
                            style={{ width: '100%' }}
                            value={selectedSubjectId}
                            onChange={handleSubjectFilterChange}
                            filterOption={(input, option) =>
                                (option?.children ?? '').toLowerCase().includes(input.toLowerCase())
                            }
                            loading={!allSubjects.length && isLoading}
                        >
                            {allSubjects.map(subject => (
                                <Option key={subject.id} value={subject.id}>
                                    {subject.name}
                                </Option>
                            ))}
                        </Select>
                    </Col>
                    <Col xs={24} md={4}>
                        <Button icon={<SortIcon />} onClick={handleSortToggle} style={{ width: '100%' }}>
                            {sortButtonText}
                        </Button>
                    </Col>
                </Row>
                <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
                    Добавить преподавателя
                </Button>
            </Space>

            <Spin spinning={isLoading}>
                <List
                    itemLayout="horizontal"
                    dataSource={filteredTeachers}
                    renderItem={(teacher) => {
                        const averageRating = ratings[teacher.id];
                        return (
                            <List.Item
                                className="list-item-margin"
                                actions={[
                                    <Tooltip title="Посмотреть отзывы о преподавателе">
                                        <Link to={`/teachers/${teacher.id}/reviews`}>
                                            <Button type="link" icon={<EyeOutlined />} />
                                        </Link>
                                    </Tooltip>,
                                    <Tooltip title="Редактировать преподавателя">
                                        <Button type="link" icon={<EditOutlined />} onClick={() => handleEdit(teacher)} />
                                    </Tooltip>,
                                    <Popconfirm
                                        title="Удалить преподавателя?"
                                        description={`Вы уверены, что хотите удалить ${getTeacherFullName(teacher)}?`}
                                        onConfirm={() => handleDelete(teacher.id)}
                                        okText="Да, удалить"
                                        cancelText="Отмена"
                                    >
                                        <Tooltip title="Удалить преподавателя">
                                            <Button type="link" danger icon={<DeleteOutlined />} />
                                        </Tooltip>
                                    </Popconfirm>,
                                ]}
                            >
                                <List.Item.Meta
                                    avatar={<Avatar style={{ backgroundColor: '#1677ff' }} icon={<TeamOutlined />} />}
                                    title={
                                        <Space>
                                            <Text strong>{getTeacherFullName(teacher)}</Text>
                                            {!loadingRatings && averageRating !== undefined && (
                                                <Tag icon={<StarFilled />} color={getRatingColor(averageRating)}>
                                                    {averageRating}
                                                </Tag>
                                            )}
                                        </Space>
                                    }
                                    description={
                                        teacher.subjects && teacher.subjects.length > 0
                                            ? <>Предметы: {teacher.subjects.map(subjectName => <Tag key={subjectName} color="geekblue">{subjectName}</Tag>)}</>
                                            : 'Предметы не назначены'
                                    }
                                />
                            </List.Item>
                        );
                    }}
                    locale={{ emptyText: (searchNameText || selectedSubjectId) ? 'Преподаватели не найдены' : 'Нет преподавателей' }}
                />
            </Spin>
            <TeacherForm
                visible={isModalVisible}
                onCreate={handleCreate}
                onUpdate={handleUpdate}
                onCancel={handleCancel}
                editingTeacher={editingTeacher}
            />
        </div>
    );
};

export default TeacherList;
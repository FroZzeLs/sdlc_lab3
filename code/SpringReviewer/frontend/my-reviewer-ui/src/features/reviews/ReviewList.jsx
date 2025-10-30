import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useParams, Link } from 'react-router-dom';
import { List, Button, Typography, Spin, Popconfirm, message, Card, Tag, Rate, Empty, Select, Row, Col, Space, Pagination } from 'antd';
import { MessageOutlined, PlusOutlined, EditOutlined, DeleteOutlined, CalendarOutlined, UserOutlined, TeamOutlined, BookOutlined, ArrowLeftOutlined, StarFilled, SortAscendingOutlined, SortDescendingOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import * as api from '../../api';
import ReviewForm from './ReviewForm';
import { useRatings } from '../../context/RatingContext';
import { getRatingColor } from '../../utils/helpers';

const { Title, Text, Paragraph } = Typography;
const { Option } = Select;

const SORT_FIELD = {
    DATE: 'date',
    TEACHER: 'teacher',
    GRADE: 'grade',
};

const SORT_ORDER = {
    ASC: 'asc',
    DESC: 'desc',
};

const PAGE_SIZE = 6;
const PAGINATION_DELAY = 400;


const ReviewList = ({ mode = 'all', title }) => {
    const { userId, teacherId } = useParams();
    const [allReviews, setAllReviews] = useState([]);
    const [processedReviews, setProcessedReviews] = useState([]);
    const [loading, setLoading] = useState(false);
    const [isModalVisible, setIsModalVisible] = useState(false);
    const [editingReview, setEditingReview] = useState(null);
    const [messageApi, contextHolder] = message.useMessage();
    const [entityName, setEntityName] = useState('');
    const [filterUsers, setFilterUsers] = useState([]);
    const [filterTeachers, setFilterTeachers] = useState([]);
    const [filterSubjects, setFilterSubjects] = useState([]);
    const [selectedAuthorId, setSelectedAuthorId] = useState(null);
    const [selectedTeacherId, setSelectedTeacherId] = useState(null);
    const [selectedSubjectId, setSelectedSubjectId] = useState(null);
    const [loadingFilters, setLoadingFilters] = useState(false);
    const [currentPage, setCurrentPage] = useState(1);
    const [isPaginating, setIsPaginating] = useState(false);

    const { ratings, isLoading: loadingRatings, fetchRatings } = useRatings();

    const [sortField, setSortField] = useState(SORT_FIELD.DATE);
    const [sortOrder, setSortOrder] = useState(SORT_ORDER.DESC);

    const getTeacherFullName = useCallback((teacher) => {
        if (!teacher) return 'N/A';
        return `${teacher.surname || ''} ${teacher.name || ''} ${teacher.patronym || ''}`.trim();
    }, []);

    const loadFilterData = useCallback(async () => {
        if (mode !== 'all' || (filterUsers.length && filterTeachers.length && filterSubjects.length)) {
            return;
        }
        setLoadingFilters(true);
        try {
            const [usersRes, teachersRes, subjectsRes] = await Promise.all([
                filterUsers.length === 0 ? api.getUsers() : Promise.resolve({ data: filterUsers }),
                filterTeachers.length === 0 ? api.getTeachers() : Promise.resolve({ data: filterTeachers }),
                filterSubjects.length === 0 ? api.getSubjects() : Promise.resolve({ data: filterSubjects }),
            ]);
            setFilterUsers(usersRes.data);
            setFilterTeachers(teachersRes.data);
            setFilterSubjects(subjectsRes.data);
        } catch (error) {
            console.error("Failed to load filter data", error);
            messageApi.error("Не удалось загрузить данные для фильтров");
        } finally {
            setLoadingFilters(false);
        }
    }, [mode, messageApi, filterUsers, filterTeachers, filterSubjects]);

    const fetchReviews = useCallback(async () => {
        setLoading(true);
        setEntityName('');
        setSelectedAuthorId(null);
        setSelectedTeacherId(null);
        setSelectedSubjectId(null);
        setCurrentPage(1);
        let pageTitle = title || "Отзывы";

        try {
            let response;
            if (mode === 'user' && userId) {
                response = await api.getReviewsByUserId(userId);
                if (response.data.length > 0 && response.data[0].author) {
                    setEntityName(response.data[0].author);
                    pageTitle = `Отзывы пользователя: ${response.data[0].author}`;
                } else {
                    try {
                        const userRes = await api.getUserById(userId);
                        setEntityName(userRes.data.username);
                        pageTitle = `Отзывы пользователя: ${userRes.data.username}`;
                    } catch (userError) {
                        console.error("Could not fetch user name", userError);
                        setEntityName(`ID: ${userId}`);
                        pageTitle = `Отзывы пользователя ID: ${userId}`;
                        if (userError?.response?.status === 404) {
                            setEntityName(`Пользователь ID: ${userId} (не найден)`);
                        }
                    }
                }
            } else if (mode === 'teacher' && teacherId) {
                response = await api.getReviewsByTeacherId(teacherId);
                if (response.data.length > 0 && response.data[0].teacher) {
                    const teacherName = getTeacherFullName(response.data[0].teacher);
                    setEntityName(teacherName);
                    pageTitle = `Отзывы о преподавателе: ${teacherName}`;
                } else {
                    try {
                        const teacherRes = await api.getTeacherById(teacherId);
                        const teacherName = getTeacherFullName(teacherRes.data);
                        setEntityName(teacherName);
                        pageTitle = `Отзывы о преподавателе: ${teacherName}`;
                    } catch (teacherError) {
                        console.error("Could not fetch teacher name", teacherError);
                        setEntityName(`ID: ${teacherId}`);
                        pageTitle = `Отзывы о преподавателе ID: ${teacherId}`;
                        if (teacherError?.response?.status === 404) {
                            setEntityName(`Преподаватель ID: ${teacherId} (не найден)`);
                        }
                    }
                }
            } else {
                response = await api.getReviews();
                pageTitle = "Все отзывы";
                loadFilterData();
            }
            setAllReviews(response.data);
            if (!title) { document.title = pageTitle; }

        } catch (error) {
            api.handleApiError(error, messageApi);
            if (error.response && error.response.status === 404) {
                setAllReviews([]);
                setProcessedReviews([]);
                if (mode === 'user' && !entityName.includes('Найден')) setEntityName(`Пользователь ID: ${userId} (не найден или нет отзывов)`);
                if (mode === 'teacher' && !entityName.includes('Найден')) setEntityName(`Преподаватель ID: ${teacherId} (не найден или нет отзывов)`);
            }
        }
        finally { setLoading(false); }
    }, [mode, userId, teacherId, messageApi, getTeacherFullName, title, entityName, loadFilterData]);

    useEffect(() => { fetchReviews(); return () => { document.title = 'Spring Reviewer'; } }, [mode, userId, teacherId]);

    useEffect(() => {
        let sortedAndFiltered = [...allReviews];

        if (mode === 'all') {
            sortedAndFiltered = sortedAndFiltered.filter(review => {
                const authorMatch = !selectedAuthorId || review.authorId === selectedAuthorId;
                const teacherMatch = !selectedTeacherId || review.teacher?.id === selectedTeacherId;
                const subjectMatch = !selectedSubjectId || review.subjectId === selectedSubjectId;
                return authorMatch && teacherMatch && subjectMatch;
            });
        }

        const orderMultiplier = sortOrder === SORT_ORDER.ASC ? 1 : -1;

        sortedAndFiltered.sort((a, b) => {
            let compareResult = 0;
            switch (sortField) {
                case SORT_FIELD.TEACHER:
                    compareResult = getTeacherFullName(a.teacher).localeCompare(getTeacherFullName(b.teacher));
                    break;
                case SORT_FIELD.GRADE:
                    compareResult = (a.grade ?? 0) - (b.grade ?? 0);
                    break;
                case SORT_FIELD.DATE:
                default:
                    compareResult = dayjs(a.date).valueOf() - dayjs(b.date).valueOf();
                    break;
            }
            return compareResult * orderMultiplier;
        });

        setProcessedReviews(sortedAndFiltered);
        if (currentPage !== 1 && allReviews.length > 0) {
            setCurrentPage(1);
        }

    }, [selectedAuthorId, selectedTeacherId, selectedSubjectId, sortField, sortOrder, allReviews, mode, getTeacherFullName]);

    const handleAdd = () => { setIsModalVisible(true); setEditingReview(null); };
    const handleEdit = (review) => {
        if (review.authorId === undefined || review.subjectId === undefined) {
            messageApi.error("Ошибка: Недостаточно данных для редактирования отзыва.");
            return;
        }
        setEditingReview(review);
        setIsModalVisible(true);
    };

    const handleDelete = async (id) => {
        try {
            await api.deleteReview(id);
            messageApi.success('Отзыв успешно удален');
            fetchReviews();
            fetchRatings();
        } catch (error) { api.handleApiError(error, messageApi); }
    };

    const handleCreate = async (values) => {
        try {
            await api.createReview(values);
            messageApi.success('Отзыв успешно создан');
            setIsModalVisible(false);
            fetchReviews();
            fetchRatings();
        } catch (error) { api.handleApiError(error, messageApi); }
    };

    const handleUpdate = async (id, values) => {
        try {
            await api.updateReview(id, values);
            messageApi.success('Отзыв успешно обновлен');
            setIsModalVisible(false);
            setEditingReview(null);
            fetchReviews();
            fetchRatings();
        } catch (error) { api.handleApiError(error, messageApi); }
    };

    const handleCancel = () => { setIsModalVisible(false); setEditingReview(null); };
    const getPageTitle = () => {
        if (title) return title;
        const baseTitle = mode === 'user' ? 'Отзывы пользователя' : 'Отзывы о преподавателе';
        return entityName ? `${baseTitle}: ${entityName}` : (loading ? 'Загрузка...' : baseTitle);
    }
    const handleAuthorFilterChange = (value) => setSelectedAuthorId(value);
    const handleTeacherFilterChange = (value) => setSelectedTeacherId(value);
    const handleSubjectFilterChange = (value) => setSelectedSubjectId(value);

    const handleSort = (field) => {
        if (field === sortField) {
            setSortOrder(prevOrder => prevOrder === SORT_ORDER.ASC ? SORT_ORDER.DESC : SORT_ORDER.ASC);
        } else {
            setSortField(field);
            setSortOrder(field === SORT_FIELD.TEACHER ? SORT_ORDER.ASC : SORT_ORDER.DESC);
        }
    };

    const getSortIcon = (field) => {
        if (sortField !== field) { return null; }
        return sortOrder === SORT_ORDER.ASC ? <SortAscendingOutlined /> : <SortDescendingOutlined />;
    };

    const paginatedReviews = useMemo(() => {
        const startIndex = (currentPage - 1) * PAGE_SIZE;
        const endIndex = startIndex + PAGE_SIZE;
        return processedReviews.slice(startIndex, endIndex);
    }, [processedReviews, currentPage]);

    const handlePageChange = (page) => {
        setIsPaginating(true);
        setTimeout(() => {
            setCurrentPage(page);
            window.scrollTo(0, 0);
            setIsPaginating(false);
        }, PAGINATION_DELAY);
    };


    return (
        <div className="list-container">
            {contextHolder}
            <Title level={4} className="section-title">
                { (mode === 'user' || mode === 'teacher') && (
                    <Link to={mode === 'user' ? '/users' : '/teachers'} style={{ marginRight: 15 }}>
                        <Button type="text" shape="circle" icon={<ArrowLeftOutlined />} />
                    </Link>
                )}
                {getPageTitle()}
            </Title>

            {mode === 'all' && (
                <Spin spinning={loadingFilters}>
                    <Card size="small" style={{ marginBottom: 16 }} bodyStyle={{paddingTop: 12, paddingBottom: 0}}>
                        <Title level={5} style={{marginTop: 0, marginBottom: 12}}>Фильтры</Title>
                        <Row gutter={[16, 16]}>
                            <Col xs={24} md={8}>
                                <Select
                                    showSearch allowClear placeholder="По автору"
                                    style={{ width: '100%' }} value={selectedAuthorId} onChange={handleAuthorFilterChange}
                                    filterOption={(input, option) => (option?.children ?? '').toLowerCase().includes(input.toLowerCase())}
                                    disabled={loadingFilters || !filterUsers.length}
                                >
                                    {filterUsers.map(user => (<Option key={user.id} value={user.id}>{user.username}</Option>))}
                                </Select>
                            </Col>
                            <Col xs={24} md={8}>
                                <Select
                                    showSearch allowClear placeholder="По преподавателю"
                                    style={{ width: '100%' }} value={selectedTeacherId} onChange={handleTeacherFilterChange}
                                    filterOption={(input, option) => (option?.children ?? '').toLowerCase().includes(input.toLowerCase())}
                                    disabled={loadingFilters || !filterTeachers.length}
                                >
                                    {filterTeachers.map(teacher => (<Option key={teacher.id} value={teacher.id}>{getTeacherFullName(teacher)}</Option>))}
                                </Select>
                            </Col>
                            <Col xs={24} md={8}>
                                <Select
                                    showSearch allowClear placeholder="По предмету"
                                    style={{ width: '100%' }} value={selectedSubjectId} onChange={handleSubjectFilterChange}
                                    filterOption={(input, option) => (option?.children ?? '').toLowerCase().includes(input.toLowerCase())}
                                    disabled={loadingFilters || !filterSubjects.length}
                                >
                                    {filterSubjects.map(subject => (<Option key={subject.id} value={subject.id}>{subject.name}</Option>))}
                                </Select>
                            </Col>
                        </Row>
                    </Card>
                </Spin>
            )}

            <Row justify="space-between" align="middle" style={{ marginBottom: 16 }} gutter={[16, 8]}>
                <Col>
                    <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
                        Добавить отзыв
                    </Button>
                </Col>
                <Col>
                    <Space wrap>
                        <Text>Сортировать:</Text>
                        <Button.Group>
                            <Button
                                type={sortField === SORT_FIELD.DATE ? "primary" : "default"}
                                icon={getSortIcon(SORT_FIELD.DATE)}
                                onClick={() => handleSort(SORT_FIELD.DATE)}
                            >
                                Дата
                            </Button>
                            <Button
                                type={sortField === SORT_FIELD.TEACHER ? "primary" : "default"}
                                icon={getSortIcon(SORT_FIELD.TEACHER)}
                                onClick={() => handleSort(SORT_FIELD.TEACHER)}
                            >
                                Преподаватель
                            </Button>
                            <Button
                                type={sortField === SORT_FIELD.GRADE ? "primary" : "default"}
                                icon={getSortIcon(SORT_FIELD.GRADE)}
                                onClick={() => handleSort(SORT_FIELD.GRADE)}
                            >
                                Оценка
                            </Button>
                        </Button.Group>
                    </Space>
                </Col>
            </Row>

            <Spin spinning={loading || isPaginating}>
                <List
                    grid={{ gutter: 16, xs: 1, sm: 1, md: 2, lg: 3, xl: 3, xxl: 3 }}
                    dataSource={paginatedReviews}
                    locale={{
                        emptyText: loading ? ' ' : (
                            (mode === 'all' && (selectedAuthorId || selectedTeacherId || selectedSubjectId))
                                ? "Отзывы по заданным фильтрам не найдены"
                                : (mode === 'all' ? "Отзывов пока нет" : "Для данного объекта отзывов не найдено")
                        )
                    }}
                    renderItem={(review) => {
                        const averageRating = review.teacher?.id ? ratings[review.teacher.id] : undefined;
                        return (
                            <List.Item key={review.id}>
                                <Card
                                    hoverable
                                    title={
                                        <Space>
                                            <TeamOutlined />
                                            {mode !== 'teacher' && review.teacher?.id ? (
                                                <Link to={`/teachers/${review.teacher.id}/reviews`}>
                                                    {getTeacherFullName(review.teacher)}
                                                </Link>
                                            ) : (
                                                getTeacherFullName(review.teacher)
                                            )}
                                            {!loadingRatings && averageRating !== undefined && (
                                                <Tag icon={<StarFilled />} color={getRatingColor(averageRating)}>
                                                    {averageRating?.toFixed(1)}
                                                </Tag>
                                            )}
                                        </Space>
                                    }
                                    actions={[
                                        <Button type="link" icon={<EditOutlined />} onClick={() => handleEdit(review)}>Редактировать</Button>,
                                        <Popconfirm
                                            title="Удалить отзыв?"
                                            description="Вы уверены, что хотите удалить этот отзыв?"
                                            onConfirm={() => handleDelete(review.id)}
                                            okText="Да, удалить"
                                            cancelText="Отмена"
                                        >
                                            <Button type="link" danger icon={<DeleteOutlined />}>Удалить</Button>
                                        </Popconfirm>,
                                    ]}
                                >
                                    <div style={{ marginBottom: '10px' }}>
                                        <Tag icon={<BookOutlined />} color="blue">{review.subjectName || 'N/A'}</Tag>
                                        {mode !== 'user' && review.authorId ? (
                                            <Tag icon={<UserOutlined />} color="purple">
                                                <Link to={`/users/${review.authorId}/reviews`} style={{ color: 'inherit' }}>
                                                    {review.author || 'N/A'}
                                                </Link>
                                            </Tag>
                                        ) : (
                                            <Tag icon={<UserOutlined />} color="purple">{review.author || 'N/A'}</Tag>
                                        )}
                                        <Tag icon={<CalendarOutlined />} color="default">
                                            {review.date ? dayjs(review.date).format('DD.MM.YYYY') : '?'}
                                        </Tag>
                                    </div>
                                    <Rate
                                        disabled
                                        allowHalf
                                        value={review.grade / 2}
                                        style={{ color: getRatingColor(review.grade), marginBottom: '10px', fontSize: 18 }}
                                    />
                                    <Text strong style={{ color: getRatingColor(review.grade) }}> ({review.grade}/10)</Text>
                                    <Paragraph ellipsis={{ rows: 3, expandable: true, symbol: 'ещё' }} style={{ marginTop: '10px' }}>
                                        {review.comment || <Text type="secondary">Комментарий отсутствует</Text>}
                                    </Paragraph>
                                </Card>
                            </List.Item>
                        );
                    }}
                />
                {processedReviews.length > PAGE_SIZE && (
                    <Pagination
                        current={currentPage}
                        pageSize={PAGE_SIZE}
                        total={processedReviews.length}
                        onChange={handlePageChange}
                        style={{ textAlign: 'center', marginTop: '24px' }}
                        showSizeChanger={false}
                        disabled={isPaginating}
                    />
                )}
                {(processedReviews.length === 0 && !loading && !isPaginating) &&
                    <Empty description={
                        (mode === 'all' && (selectedAuthorId || selectedTeacherId || selectedSubjectId))
                            ? "Отзывы по заданным фильтрам не найдены"
                            : (mode === 'all' ? "Отзывов пока нет" : "Для данного объекта отзывов не найдено")
                    } />
                }
            </Spin>
            <ReviewForm
                visible={isModalVisible}
                onCreate={handleCreate}
                onUpdate={handleUpdate}
                onCancel={handleCancel}
                editingReview={editingReview}
            />
        </div>
    );
};

export default ReviewList;
import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Row, Col, Card, Typography, Spin, Button, message, DatePicker, Alert, Progress, Table, Space, Tag } from 'antd';
import { TeamOutlined, BookOutlined, UserOutlined, PlusOutlined, FileTextOutlined, LoadingOutlined, CheckCircleOutlined, CloseCircleOutlined, DownloadOutlined, BarChartOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import * as api from '../../api';

const { Title, Text, Paragraph } = Typography;

const LogGenerationStatus = {
    PENDING: 'PENDING',
    RUNNING: 'RUNNING',
    COMPLETED: 'COMPLETED',
    FAILED: 'FAILED'
};

const HomePage = () => {
    const [messageApi, contextHolder] = message.useMessage();
    const navigate = useNavigate();

    const [logDate, setLogDate] = useState(null);
    const [logTask, setLogTask] = useState(null);
    const [logLoading, setLogLoading] = useState(false);
    const [logError, setLogError] = useState(null);
    const pollingIntervalRef = useRef(null);

    const [visitMetrics, setVisitMetrics] = useState([]);
    const [loadingMetrics, setLoadingMetrics] = useState(false);


    const fetchVisitMetrics = useCallback(async () => {
        setLoadingMetrics(true);
        try {
            const response = await api.getUrlVisitCounts();
            const formattedMetrics = Object.entries(response.data)
                .map(([url, count], index) => ({ key: index, url, count }))
                .sort((a, b) => b.count - a.count);
            setVisitMetrics(formattedMetrics);
        } catch (error) {
            console.error("Failed to fetch visit metrics:", error);
        } finally {
            setLoadingMetrics(false);
        }
    }, [messageApi]);

    const stopPolling = useCallback(() => {
        if (pollingIntervalRef.current) {
            clearInterval(pollingIntervalRef.current);
            pollingIntervalRef.current = null;
        }
    }, []);

    const pollStatus = useCallback(async (taskId) => {
        if (!taskId) return;
        try {
            const response = await api.getLogGenerationStatus(taskId);
            const { status, errorMessage, downloadUrl } = response.data;

            setLogTask(prev => {
                if (!prev || prev.id !== taskId) return prev;
                return { ...prev, status, errorMessage, downloadUrl };
            });

            if (status === LogGenerationStatus.COMPLETED || status === LogGenerationStatus.FAILED) {
                stopPolling();
                setLogLoading(false);
                if (status === LogGenerationStatus.FAILED) {
                    setLogError(errorMessage || 'Генерация лога завершилась с ошибкой.');
                }
                if (status === LogGenerationStatus.COMPLETED && !downloadUrl) {
                    setLogError('Генерация завершена, но ссылка на скачивание отсутствует.');
                    messageApi.warning('Генерация завершена, но ссылка на скачивание не получена от сервера.', 5);
                }
            }
        } catch (error) {
            console.error("Polling error:", error);
            const errMsg = api.handleApiError(error, null, 'Ошибка при проверке статуса задачи');
            setLogError(errMsg + '. Проверка остановлена.');
            stopPolling();
            setLogLoading(false);
            setLogTask(prev => (prev && prev.id === taskId) ? {...prev, status: LogGenerationStatus.FAILED } : null);
        }
    }, [stopPolling, messageApi]);


    const handleStartGeneration = async () => {
        if (!logDate) {
            messageApi.warning('Пожалуйста, выберите дату для генерации лога.');
            return;
        }
        stopPolling();
        setLogLoading(true);
        setLogError(null);
        setLogTask(null);
        const formattedDate = dayjs(logDate).format('YYYY-MM-DD');

        try {
            const response = await api.startLogGeneration(formattedDate);
            const { taskId, status, statusUrl } = response.data;
            if (!taskId) {
                throw new Error("Не удалось получить ID задачи от сервера.");
            }
            messageApi.success(`Задача генерации лога ${taskId} запущена.`);
            setLogTask({ id: taskId, status: status || LogGenerationStatus.PENDING, date: formattedDate, downloadUrl: null, errorMessage: null });

            setTimeout(() => pollStatus(taskId), 500);
            pollingIntervalRef.current = setInterval(() => pollStatus(taskId), 3000);

        } catch (error) {
            const errorMsg = api.handleApiError(error, messageApi, 'Не удалось запустить генерацию лога');
            setLogError(errorMsg);
            setLogLoading(false);
        }
    };

    useEffect(() => {
        fetchVisitMetrics();
        return () => {
            stopPolling();
        };
    }, [fetchVisitMetrics, stopPolling]);


    const getLogStatusProps = (status) => {
        switch (status) {
            case LogGenerationStatus.PENDING:
                return { text: 'Ожидание', icon: <LoadingOutlined />, spin: true, color: 'default', percent: 10 };
            case LogGenerationStatus.RUNNING:
                return { text: 'В процессе', icon: <LoadingOutlined />, spin: true, color: 'processing', percent: 50 };
            case LogGenerationStatus.COMPLETED:
                return { text: 'Завершено', icon: <CheckCircleOutlined />, spin: false, color: 'success', percent: 100 };
            case LogGenerationStatus.FAILED:
                return { text: 'Ошибка', icon: <CloseCircleOutlined />, spin: false, color: 'error', percent: 100, status: 'exception' };
            default:
                return { text: 'Неизвестно', icon: null, spin: false, color: 'default', percent: 0 };
        }
    };

    const metricsColumns = [
        { title: 'URL Шаблон', dataIndex: 'url', key: 'url', render: (text) => <Text code style={{whiteSpace: 'nowrap'}}>{text}</Text> },
        { title: 'Кол-во посещений', dataIndex: 'count', key: 'count', width: 150, align: 'right', sorter: (a, b) => a.count - b.count, defaultSortOrder: 'descend' },
    ];

    const logStatusProps = logTask ? getLogStatusProps(logTask.status) : null;

    return (
        <div>
            {contextHolder}
            <Title level={2} style={{ marginBottom: '24px' }}>Панель управления</Title>

            <Row gutter={[16, 16]} style={{ marginBottom: '24px' }}>
                <Col xs={24} lg={16}>
                    {}
                    <Card title="Генерация файла логов" variant="borderless" extra={<FileTextOutlined />}>
                        <Row gutter={16} align="bottom">
                            <Col flex="auto">
                                <DatePicker
                                    placeholder="Выберите дату лога"
                                    onChange={(date) => setLogDate(date)}
                                    value={logDate}
                                    style={{ width: '100%' }}
                                    disabled={logLoading}
                                    allowClear={false}
                                    inputReadOnly={true}
                                />
                            </Col>
                            <Col flex="none">
                                <Button
                                    type="primary"
                                    onClick={handleStartGeneration}
                                    loading={logLoading && logTask?.status !== LogGenerationStatus.COMPLETED && logTask?.status !== LogGenerationStatus.FAILED}
                                    disabled={!logDate || logLoading}
                                >
                                    Сгенерировать
                                </Button>
                            </Col>
                        </Row>
                        {logError && !logLoading && (
                            <Alert message={logError} type="error" showIcon style={{marginTop: '16px'}} closable onClose={() => setLogError(null)}/>
                        )}
                        {logTask && !logError && logStatusProps && (
                            <div style={{marginTop: '16px'}}>
                                <Text strong>Задача: </Text><Text code>{logTask.id}</Text> ({logTask.date})<br/>
                                <Progress
                                    percent={logStatusProps.percent}
                                    status={logStatusProps.status || (logLoading && logTask?.status !== LogGenerationStatus.COMPLETED && logTask?.status !== LogGenerationStatus.FAILED ? 'active' : 'normal')}
                                    strokeColor={logStatusProps.color === 'error' ? undefined : (logStatusProps.color === 'success' ? undefined : null)}
                                    format={() => (
                                        <Tag icon={logStatusProps.spin ? <LoadingOutlined spin /> : logStatusProps.icon} color={logStatusProps.color}>
                                            {logStatusProps.text}
                                        </Tag>
                                    )}
                                    style={{marginTop: '8px'}}
                                />
                                {logTask.status === LogGenerationStatus.FAILED && logTask.errorMessage && (
                                    <Paragraph type="danger" style={{marginTop: '5px', fontSize: '12px'}}><Text strong>Причина:</Text> {logTask.errorMessage}</Paragraph>
                                )}
                                {logTask.status === LogGenerationStatus.COMPLETED && logTask.downloadUrl && (
                                    <Button
                                        type="primary"
                                        icon={<DownloadOutlined />}
                                        href={`/api/logs/generate/${logTask.id}/download`}
                                        style={{marginTop: '16px'}}
                                    >
                                        Скачать файл
                                    </Button>
                                )}
                            </div>
                        )}
                    </Card>
                </Col>
                <Col xs={24} lg={8}>
                    {}
                    <Card title="Быстрые действия" variant="borderless">
                        <Space direction="vertical" style={{ width: '100%' }}>
                            <Button type="primary" icon={<PlusOutlined />} block onClick={() => navigate('/reviews')}>
                                Добавить отзыв
                            </Button>
                            <Button icon={<TeamOutlined />} block onClick={() => navigate('/teachers')}>
                                Список преподавателей
                            </Button>
                            <Button icon={<BookOutlined />} block onClick={() => navigate('/subjects')}>
                                Список предметов
                            </Button>
                            <Button icon={<UserOutlined />} block onClick={() => navigate('/users')}>
                                Список пользователей
                            </Button>
                        </Space>
                    </Card>
                </Col>
            </Row>


            <Row gutter={[16, 16]}>
                <Col xs={24} md={24}>
                    {/* Заменяем bordered={false} на variant="borderless" */}
                    <Card title="Статистика посещений URL" variant="borderless" extra={<BarChartOutlined />}>
                        <Spin spinning={loadingMetrics}>
                            <Table
                                dataSource={visitMetrics}
                                columns={metricsColumns}
                                pagination={{ pageSize: 5, hideOnSinglePage: true, size: 'small' }}
                                size="small"
                                scroll={{ x: 'max-content' }}
                                locale={{ emptyText: 'Нет данных о посещениях' }}
                            />
                        </Spin>
                    </Card>
                </Col>
            </Row>
        </div>
    );
};

export default HomePage;
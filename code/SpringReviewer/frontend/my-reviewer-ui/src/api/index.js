import axios from 'axios';
import dayjs from 'dayjs';

const apiClient = axios.create({
    baseURL: '/api',
    headers: {
        'Content-Type': 'application/json',
    },
});

export const getUsers = () => apiClient.get('/users');
export const getUserById = (id) => apiClient.get(`/users/${id}`);
export const createUser = (userData) => apiClient.post('/users', userData);
export const updateUser = (id, userData) => apiClient.put(`/users/${id}`, userData);
export const deleteUser = (id) => apiClient.delete(`/users/${id}`);

export const getTeachers = () => apiClient.get('/teachers');
export const getTeacherById = (id) => apiClient.get(`/teachers/${id}`);
export const createTeacher = (teacherData) => apiClient.post('/teachers', teacherData);
export const updateTeacher = (id, teacherData) => apiClient.put(`/teachers/${id}`, teacherData);
export const deleteTeacher = (id) => apiClient.delete(`/teachers/${id}`);
export const assignSubjectToTeacher = (teacherId, subjectId) => apiClient.post(`/teachers/${teacherId}/subjects/${subjectId}`);
export const removeSubjectFromTeacher = (teacherId, subjectId) => apiClient.delete(`/teachers/${teacherId}/subjects/${subjectId}`);

export const getSubjects = () => apiClient.get('/subjects');
export const createSubject = (subjectData) => apiClient.post('/subjects', subjectData);
export const updateSubject = (id, subjectData) => apiClient.put(`/subjects/${id}`, subjectData);
export const deleteSubject = (id) => apiClient.delete(`/subjects/${id}`);

export const getReviews = () => apiClient.get('/reviews');
export const getReviewsByUserId = (userId) => apiClient.get(`/reviews/user/${userId}`);
export const getReviewsByTeacherId = (teacherId) => apiClient.get(`/reviews/teacher/${teacherId}`);

const formatReviewData = (reviewData) => ({
    ...reviewData,
    date: reviewData.date ? dayjs(reviewData.date).format('YYYY-MM-DD') : null,
    userId: parseInt(reviewData.userId, 10),
    teacherId: parseInt(reviewData.teacherId, 10),
    subjectId: parseInt(reviewData.subjectId, 10),
    grade: parseInt(reviewData.grade, 10),
});
export const createReview = (reviewData) => apiClient.post('/reviews', formatReviewData(reviewData));
export const updateReview = (id, reviewData) => apiClient.put(`/reviews/${id}`, formatReviewData(reviewData));
export const deleteReview = (id) => apiClient.delete(`/reviews/${id}`);

export const startLogGeneration = (date) => apiClient.post(`/logs/generate?date=${date}`);
export const getLogGenerationStatus = (id) => apiClient.get(`/logs/generate/${id}/status`);

export const getUrlVisitCounts = () => apiClient.get('/metrics/visits/by-url');
export const getCounts = () => apiClient.get('/metrics/counts');
export const getRecentReviews = (limit = 5) => apiClient.get(`/reviews/recent?limit=${limit}`);

export const handleApiError = (error, messageApi, defaultMessage = 'Произошла ошибка') => {
    console.error("API Error:", error);
    let errorMessage = defaultMessage;
    if (error.response) {
        console.error("Error response data:", error.response.data);
        console.error("Error response status:", error.response.status);
        console.error("Error response headers:", error.response.headers);
        if (typeof error.response.data === 'string' && error.response.data) {
            errorMessage = error.response.data;
        } else if (error.response.data && (error.response.data.message || error.response.data.error || error.response.data.detail)) {
            errorMessage = error.response.data.message || error.response.data.error || error.response.data.detail;
        } else if (error.response.status === 404) {
            errorMessage = 'Запрашиваемый ресурс не найден (404).';
        } else if (error.response.status === 400) {
            errorMessage = 'Неверные данные запроса (400).';
            if (error.response.data && Array.isArray(error.response.data.errors)) {
                const validationErrors = error.response.data.errors.map(e => `${e.field}: ${e.defaultMessage}`).join('; ');
                errorMessage += ` Детали: ${validationErrors}`;
            } else if (error.response.data && typeof error.response.data === 'object'){
                const details = Object.entries(error.response.data)
                    .map(([key, value]) => `${key}: ${value}`)
                    .join('; ');
                if(details) errorMessage += ` Детали: ${details}`;
            }
        } else if (error.response.status === 409) {
            errorMessage = 'Конфликт данных (409).';
        } else if (error.response.status === 422) {
            errorMessage = 'Ошибка обработки данных (422).';
            if (error.response.data && error.response.data.message) {
                errorMessage += ` ${error.response.data.message}`;
            }
        }
        else {
            errorMessage = `Ошибка сервера: ${error.response.status || 'Статус неизвестен'}.`;
        }
    } else if (error.request) {
        console.error("Error request:", error.request);
        errorMessage = 'Не удалось подключиться к серверу. Проверьте бэкенд и сеть.';
    } else {
        console.error('Error message:', error.message);
        errorMessage = `Ошибка при настройке запроса: ${error.message}`;
    }
    if (messageApi?.error) {
        messageApi.error(errorMessage);
    } else {
        console.error("Fallback error display:", errorMessage);
    }
    return errorMessage;
};
import React, { useEffect, useCallback } from 'react';
import { Modal, Form, Input, Button } from 'antd';

const subjectNameRegex = /^[А-ЯЁA-Zа-яёa-z\s]+$/;
const subjectNameMessage = 'Название может содержать только буквы и пробелы';

const SubjectForm = ({ visible, onCreate, onUpdate, onCancel, editingSubject }) => {
    const [form] = Form.useForm();
    const isEditing = !!editingSubject;

    useEffect(() => {
        if (visible) {
            if (isEditing) {
                form.setFieldsValue({ name: editingSubject.name });
            } else {
                form.resetFields();
            }
        } else {
            form.resetFields();
        }
    }, [visible, editingSubject, isEditing, form]);

    const handleOk = useCallback(() => {
        form
            .validateFields()
            .then((values) => {
                if (isEditing) {
                    onUpdate(editingSubject.id, values);
                } else {
                    onCreate(values);
                }
            })
            .catch((info) => {
                console.log('Validate Failed:', info);
                if (info.errorFields && info.errorFields.length > 0) {
                    form.scrollToField(info.errorFields[0].name[0]);
                }
            });
    }, [form, isEditing, editingSubject, onCreate, onUpdate]);

    useEffect(() => {
        const handleKeyDown = (event) => {
            if (visible && (event.ctrlKey || event.metaKey) && event.key === 'Enter') {
                event.preventDefault();
                handleOk();
            }
        };

        if (visible) {
            window.addEventListener('keydown', handleKeyDown);
        }

        return () => {
            window.removeEventListener('keydown', handleKeyDown);
        };
    }, [visible, handleOk]);

    return (
        <Modal
            open={visible}
            title={isEditing ? 'Редактировать предмет' : 'Добавить предмет'}
            okText={isEditing ? 'Сохранить' : 'Создать'}
            cancelText="Отмена"
            onCancel={onCancel}
            onOk={handleOk}
            destroyOnClose
            maskClosable={false}
        >
            <Form form={form} layout="vertical" name="subject_form" autoComplete="off">
                <Form.Item
                    name="name"
                    label="Название предмета"
                    rules={[
                        { required: true, message: 'Пожалуйста, введите название!' },
                        { min: 2, message: 'Минимум 2 символа' },
                        { max: 100, message: 'Максимум 100 символов' },
                        { pattern: subjectNameRegex, message: subjectNameMessage },
                        { whitespace: true, message: 'Название не может состоять только из пробелов' },
                        { pattern: /^[^\s]+(\s+[^\s]+)*$/, message: 'Уберите лишние пробелы в начале/конце' }
                    ]}
                >
                    <Input maxLength={100}/>
                </Form.Item>
            </Form>
        </Modal>
    );
};

export default SubjectForm;
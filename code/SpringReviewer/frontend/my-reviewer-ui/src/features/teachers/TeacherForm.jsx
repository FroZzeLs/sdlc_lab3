import React, { useEffect, useState, useCallback } from 'react';
import { Modal, Form, Input, Button, Select, Spin, message } from 'antd';
import * as api from '../../api';

const { Option } = Select;

const nameRegex = /^[А-ЯЁA-Z][а-яёa-z]+(?:-[А-ЯЁA-Z][а-яёa-z]+)?$/;
const nameMessage = 'Должно начинаться с заглавной буквы, содержать только буквы и один необязательный дефис для двойных имен/фамилий (напр., Анна-Мария)';

const TeacherForm = ({ visible, onCreate, onUpdate, onCancel, editingTeacher }) => {
  const [form] = Form.useForm();
  const [subjects, setSubjects] = useState([]);
  const [loadingSubjects, setLoadingSubjects] = useState(false);
  const [messageApi, contextHolder] = message.useMessage();
  const isEditing = !!editingTeacher;

  const fetchSubjects = useCallback(async () => {
    setLoadingSubjects(true);
    try {
      const response = await api.getSubjects();
      setSubjects(response.data);
    } catch (error) {
      console.error("Failed to fetch subjects:", error);
      messageApi.error('Не удалось загрузить список предметов');
    } finally {
      setLoadingSubjects(false);
    }
  }, [messageApi]);


  useEffect(() => {
    if (visible) {
      fetchSubjects();

      if (isEditing && editingTeacher) {
        form.setFieldsValue({
          surname: editingTeacher.surname,
          name: editingTeacher.name,
          patronym: editingTeacher.patronym,
        });
      } else {
        form.resetFields();
      }
    } else {
      form.resetFields();
    }
  }, [visible, editingTeacher, isEditing, form, fetchSubjects]);

  useEffect(() => {
    if (visible && isEditing && editingTeacher && subjects.length > 0) {
      const teacherSubjectIds = editingTeacher.subjects
          .map(subjName => subjects.find(s => s.name === subjName)?.id)
          .filter(id => id !== null);
      form.setFieldsValue({ subjectIds: teacherSubjectIds });
    }
  }, [visible, isEditing, editingTeacher, subjects, form]);


  const handleOk = useCallback(async () => {
    try {
      const values = await form.validateFields();
      const teacherData = {
        surname: values.surname,
        name: values.name,
        patronym: values.patronym,
      };
      const selectedSubjectIds = values.subjectIds || [];

      if (isEditing) {
        await onUpdate(editingTeacher.id, teacherData, selectedSubjectIds);
      } else {
        await onCreate(teacherData, selectedSubjectIds);
      }
    } catch (info) {
      if (info.errorFields) {
        console.log('Validate Failed:', info);
        if (info.errorFields.length > 0) {
          form.scrollToField(info.errorFields[0].name[0]);
        }
      } else {
        console.error('Submit failed:', info);
      }
    }
  }, [form, isEditing, editingTeacher, subjects, onCreate, onUpdate]);

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
      <>
        {contextHolder}
        <Modal
            open={visible}
            title={isEditing ? 'Редактировать преподавателя' : 'Добавить преподавателя'}
            okText={isEditing ? 'Сохранить' : 'Создать'}
            cancelText="Отмена"
            onCancel={onCancel}
            onOk={handleOk}
            confirmLoading={loadingSubjects}
            destroyOnClose
            width={600}
            maskClosable={false}
        >
          <Spin spinning={loadingSubjects} tip="Загрузка предметов...">
            <Form form={form} layout="vertical" name="teacher_form" autoComplete="off">
              <Form.Item
                  name="surname"
                  label="Фамилия"
                  rules={[
                    { required: true, message: 'Пожалуйста, введите фамилию!' },
                    { max: 30, message: 'Максимум 30 символов' },
                    { pattern: nameRegex, message: nameMessage }
                  ]}
              >
                <Input maxLength={30} />
              </Form.Item>
              <Form.Item
                  name="name"
                  label="Имя"
                  rules={[
                    { required: true, message: 'Пожалуйста, введите имя!' },
                    { max: 30, message: 'Максимум 30 символов' },
                    { pattern: nameRegex, message: nameMessage }
                  ]}
              >
                <Input maxLength={30}/>
              </Form.Item>
              <Form.Item
                  name="patronym"
                  label="Отчество"
                  rules={[
                    { max: 30, message: 'Максимум 30 символов' },
                    { pattern: nameRegex, message: nameMessage }
                  ]}
              >
                <Input maxLength={30}/>
              </Form.Item>
              <Form.Item
                  name="subjectIds"
                  label="Предметы"
              >
                <Select
                    mode="multiple"
                    allowClear
                    style={{ width: '100%' }}
                    placeholder="Выберите предметы"
                    loading={loadingSubjects}
                    filterOption={(input, option) =>
                        (option?.children ?? '').toLowerCase().includes(input.toLowerCase())
                    }
                >
                  {subjects.map(subject => (
                      <Option key={subject.id} value={subject.id}>
                        {subject.name}
                      </Option>
                  ))}
                </Select>
              </Form.Item>
            </Form>
          </Spin>
        </Modal>
      </>
  );
};

export default TeacherForm;
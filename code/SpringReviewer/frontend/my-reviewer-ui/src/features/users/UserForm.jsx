import React, { useEffect, useCallback } from 'react';
import { Modal, Form, Input, Button } from 'antd';

const UserForm = ({ visible, onCreate, onUpdate, onCancel, editingUser }) => {
  const [form] = Form.useForm();
  const isEditing = !!editingUser;

  useEffect(() => {
    if (visible) {
      if (isEditing) {
        form.setFieldsValue({ username: editingUser.username });
      } else {
        form.resetFields();
      }
    } else {
      form.resetFields();
    }
  }, [visible, editingUser, isEditing, form]);

  const handleOk = useCallback(() => {
    form
        .validateFields()
        .then((values) => {
          if (isEditing) {
            onUpdate(editingUser.id, values);
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
  }, [form, isEditing, editingUser, onCreate, onUpdate]);

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
          title={isEditing ? 'Редактировать пользователя' : 'Добавить пользователя'}
          okText={isEditing ? 'Сохранить' : 'Создать'}
          cancelText="Отмена"
          onCancel={onCancel}
          onOk={handleOk}
          destroyOnClose
          maskClosable={false}
      >
        <Form form={form} layout="vertical" name="user_form" autoComplete="off">
          <Form.Item
              name="username"
              label="Имя пользователя (логин)"
              rules={[
                { required: true, message: 'Пожалуйста, введите имя пользователя!' },
                { min: 3, message: 'Минимум 3 символа' },
                { max: 50, message: 'Максимум 50 символов' },
              ]}
          >
            <Input />
          </Form.Item>
        </Form>
      </Modal>
  );
};

export default UserForm;
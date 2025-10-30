package by.frozzel.springreviewer.mapper;

import by.frozzel.springreviewer.dto.SubjectCreateDto;
import by.frozzel.springreviewer.dto.SubjectDisplayDto;
import by.frozzel.springreviewer.model.Subject;
import by.frozzel.springreviewer.model.Teacher; // Добавить импорт
import java.util.Collections;
import java.util.List; // Добавить импорт
import java.util.Objects; // Добавить импорт
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class SubjectMapper {
    public Subject toEntity(SubjectCreateDto dto) {
        Subject subject = new Subject();
        subject.setName(dto.getName());
        subject.setTeachers(Collections.emptyList());
        return subject;
    }

    public SubjectDisplayDto toDto(Subject subject) {
        if (subject == null) {
            return null;
        }

        List<String> teacherNames = (subject.getTeachers() != null)
                ? subject.getTeachers().stream()
                .filter(Objects::nonNull)
                .map(this::formatTeacherName) // Используем отдельный метод для форматирования
                .toList()
                : Collections.emptyList();

        return new SubjectDisplayDto(
                subject.getId(),
                subject.getName(),
                teacherNames
        );
    }

    // Вспомогательный метод для безопасного форматирования имени учителя
    private String formatTeacherName(Teacher teacher) {
        if (teacher == null) {
            return "";
        }
        return String.format("%s %s %s",
                        teacher.getSurname() != null ? teacher.getSurname() : "",
                        teacher.getName() != null ? teacher.getName() : "",
                        teacher.getPatronym() != null ? teacher.getPatronym() : "")
                .replace("  ", " ").trim();
    }
}
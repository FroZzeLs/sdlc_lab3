package by.frozzel.springreviewer.mapper;

import by.frozzel.springreviewer.dto.TeacherCreateDto;
import by.frozzel.springreviewer.dto.TeacherDisplayDto;
import by.frozzel.springreviewer.model.Subject;
import by.frozzel.springreviewer.model.Teacher;
import java.util.ArrayList;
import java.util.Collections; // Добавить импорт
import java.util.List; // Добавить импорт
import java.util.Objects; // Добавить импорт
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class TeacherMapper {

    public Teacher toEntity(TeacherCreateDto dto) {
        Teacher teacher = new Teacher();
        teacher.setSurname(dto.getSurname());
        teacher.setName(dto.getName());
        teacher.setPatronym(dto.getPatronym());
        teacher.setSubjects(new ArrayList<>());
        return teacher;
    }

    public TeacherDisplayDto toDto(Teacher teacher) {
        if (teacher == null) {
            return null;
        }

        List<String> subjectNames = (teacher.getSubjects() != null)
                ? teacher.getSubjects().stream()
                .filter(Objects::nonNull)
                .map(Subject::getName)
                .filter(Objects::nonNull) // Добавим проверку имени предмета
                .toList()
                : Collections.emptyList();

        return TeacherDisplayDto.builder()
                .id(teacher.getId())
                .surname(teacher.getSurname())
                .name(teacher.getName())
                .patronym(teacher.getPatronym())
                .subjects(subjectNames)
                .build();
    }
}
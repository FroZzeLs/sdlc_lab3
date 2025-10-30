package by.frozzel.springreviewer.service;

import by.frozzel.springreviewer.dto.TeacherCreateDto;
import by.frozzel.springreviewer.dto.TeacherDisplayDto;
import by.frozzel.springreviewer.exception.BadRequestException;
import by.frozzel.springreviewer.exception.ResourceNotFoundException;
import by.frozzel.springreviewer.mapper.TeacherMapper;
import by.frozzel.springreviewer.model.Subject;
import by.frozzel.springreviewer.model.Teacher;
import by.frozzel.springreviewer.repository.SubjectRepository;
import by.frozzel.springreviewer.repository.TeacherRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherService {
    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherMapper teacherMapper;

    private static final String TEACHER_RESOURCE = "Teacher";
    private static final String SUBJECT_RESOURCE = "Subject";
    private static final String ID_FIELD = "id";
    private static final String NAME_FIELD = "name";
    private static final String SURNAME_FIELD = "surname";

    @Transactional(readOnly = true)
    public List<TeacherDisplayDto> getAllTeachers() {
        return teacherRepository.findAll().stream()
                .map(teacherMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public TeacherDisplayDto getTeacherById(Integer id) {
        return teacherRepository.findById(id)
                .map(teacherMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(TEACHER_RESOURCE, ID_FIELD, id));
    }

    @Transactional
    public TeacherDisplayDto createTeacher(TeacherCreateDto teacherCreateDto) {
        Teacher teacher = teacherMapper.toEntity(teacherCreateDto);
        Teacher savedTeacher = teacherRepository.save(teacher);
        return teacherMapper.toDto(savedTeacher);
    }

    @Transactional
    public List<TeacherDisplayDto> createTeachersBulk(List<TeacherCreateDto> teacherCreateDtos) {
        List<Teacher> teachersToSave = teacherCreateDtos.stream()
                .map(teacherMapper::toEntity)
                .toList();

        List<Teacher> savedTeachers = teacherRepository.saveAll(teachersToSave);

        return savedTeachers.stream()
                .map(teacherMapper::toDto)
                .toList();
    }

    @Transactional
    public TeacherDisplayDto updateTeacher(Integer id, TeacherCreateDto teacherCreateDto) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(TEACHER_RESOURCE, ID_FIELD, id));
        teacher.setSurname(teacherCreateDto.getSurname());
        teacher.setName(teacherCreateDto.getName());
        teacher.setPatronym(teacherCreateDto.getPatronym());
        Teacher updatedTeacher = teacherRepository.save(teacher);
        return teacherMapper.toDto(updatedTeacher);
    }

    @Transactional
    public void deleteTeacher(Integer id) {
        teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(TEACHER_RESOURCE, ID_FIELD, id));
        teacherRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public TeacherDisplayDto getTeacherByFullName(String surname, String name) {
        return teacherRepository.findBySurnameAndNameIgnoreCase(surname, name)
                .map(teacherMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("%s not found with %s: '%s' and %s: '%s'",
                                TEACHER_RESOURCE, SURNAME_FIELD, surname, NAME_FIELD, name)));
    }

    @Transactional
    public void assignSubjectToTeacher(int teacherId, int subjectId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException(TEACHER_RESOURCE,
                        ID_FIELD, teacherId));

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException(SUBJECT_RESOURCE,
                        ID_FIELD, subjectId));

        boolean alreadyAssigned = teacher.getSubjects().stream()
                .anyMatch(s -> s.getId() == subject.getId());

        if (!alreadyAssigned) {
            teacher.getSubjects().add(subject);
            teacherRepository.save(teacher);
            log.info("Assigned subject {} to teacher {}", subjectId, teacherId);
        } else {
            log.info("Teacher {} already teaches subject {}", teacherId, subjectId);
        }
    }

    @Transactional
    public void removeSubjectFromTeacher(int teacherId, int subjectId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException(TEACHER_RESOURCE,
                        ID_FIELD, teacherId));

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException(SUBJECT_RESOURCE,
                        ID_FIELD, subjectId));

        if (teacher.getSubjects().remove(subject)) {
            teacherRepository.save(teacher);
        } else {
            throw new BadRequestException(
                    String.format("Teacher %d does not teach subject %d", teacherId, subjectId));
        }
    }

    @Transactional(readOnly = true)
    public List<TeacherDisplayDto> getTeachersBySubjectName(String subjectName) {
        List<Teacher> teachers = teacherRepository.findTeachersBySubjectName(subjectName);
        if (teachers.isEmpty()) {
            throw new ResourceNotFoundException("No teachers found teaching subject: "
                    + subjectName);
        }
        return teachers.stream()
                .map(teacherMapper::toDto)
                .toList();
    }
}
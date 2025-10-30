package by.frozzel.springreviewer.service;

import by.frozzel.springreviewer.dto.SubjectCreateDto;
import by.frozzel.springreviewer.dto.SubjectDisplayDto;
import by.frozzel.springreviewer.exception.BadRequestException;
import by.frozzel.springreviewer.exception.ResourceNotFoundException;
import by.frozzel.springreviewer.mapper.SubjectMapper;
import by.frozzel.springreviewer.model.Subject;
import by.frozzel.springreviewer.model.Teacher;
import by.frozzel.springreviewer.repository.ReviewRepository;
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
public class SubjectService {
    private final SubjectRepository subjectRepository;
    private final SubjectMapper subjectMapper;
    private final ReviewRepository reviewRepository;
    private final TeacherRepository teacherRepository;

    private static final String SUBJECT_RESOURCE = "Subject";
    private static final String ID_FIELD = "id";
    private static final String NAME_FIELD = "name";

    @Transactional
    public SubjectDisplayDto createSubject(SubjectCreateDto dto) {
        Subject subject = subjectMapper.toEntity(dto);
        Subject savedSubject = subjectRepository.save(subject);
        return subjectMapper.toDto(savedSubject);
    }

    @Transactional(readOnly = true)
    public List<SubjectDisplayDto> getAllSubjects() {
        return subjectRepository.findAll().stream()
                .map(subjectMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SubjectDisplayDto getSubjectById(Integer id) {
        return subjectRepository.findById(id)
                .map(subjectMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(SUBJECT_RESOURCE, ID_FIELD, id));
    }

    @Transactional(readOnly = true)
    public SubjectDisplayDto getSubjectByName(String name) {
        return subjectRepository.findByNameIgnoreCase(name)
                .map(subjectMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(SUBJECT_RESOURCE,
                        NAME_FIELD, name));
    }

    @Transactional
    public SubjectDisplayDto updateSubject(Integer id, SubjectCreateDto dto) {
        Subject existingSubject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(SUBJECT_RESOURCE,
                        ID_FIELD, id));

        existingSubject.setName(dto.getName());
        Subject updatedSubject = subjectRepository.save(existingSubject);
        return subjectMapper.toDto(updatedSubject);
    }

    @Transactional
    public void deleteSubject(int subjectId) {
        if (subjectId <= 0) {
            throw new BadRequestException("Subject ID must be a positive number");
        }

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException(SUBJECT_RESOURCE,
                        ID_FIELD, subjectId));

        List<Teacher> teachersToRemoveFrom = List.copyOf(subject.getTeachers());
        for (Teacher teacher : teachersToRemoveFrom) {
            teacher.getSubjects().remove(subject);
            teacherRepository.save(teacher);
        }
        subject.getTeachers().clear();

        reviewRepository.deleteBySubjectId(subjectId);
        subjectRepository.delete(subject);
    }
}
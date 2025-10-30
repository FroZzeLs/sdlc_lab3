package by.frozzel.springreviewer.mapper;

import by.frozzel.springreviewer.dto.ReviewCreateDto;
import by.frozzel.springreviewer.dto.ReviewDisplayDto;
import by.frozzel.springreviewer.dto.TeacherDisplayDto;
import by.frozzel.springreviewer.exception.ResourceNotFoundException;
import by.frozzel.springreviewer.model.Review;
import by.frozzel.springreviewer.model.Subject;
import by.frozzel.springreviewer.model.Teacher;
import by.frozzel.springreviewer.model.User;
import by.frozzel.springreviewer.repository.SubjectRepository;
import by.frozzel.springreviewer.repository.TeacherRepository;
import by.frozzel.springreviewer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewMapper {
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherMapper teacherMapper;

    public Review toEntity(ReviewCreateDto dto) {
        Review review = new Review();
        review.setDate(dto.getDate());
        review.setGrade(dto.getGrade());
        review.setComment(dto.getComment());

        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: "
                        + dto.getUserId())); // Лучше использовать кастомное исключение
        Teacher teacher = teacherRepository.findById(dto.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with ID: "
                        + dto.getTeacherId()));
        Subject subject = subjectRepository.findById(dto.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: "
                        + dto.getSubjectId()));

        review.setUser(user);
        review.setTeacher(teacher);
        review.setSubject(subject);

        return review;
    }

    public ReviewDisplayDto toDto(Review review) {
        if (review == null) {
            return null;
        }

        Integer authorId = (review.getUser() != null) ? review.getUser().getId() : null;
        String authorUsername = (review.getUser() != null) ? review.getUser().getUsername() : null;

        TeacherDisplayDto teacherDto = (review.getTeacher() != null) ? teacherMapper
                .toDto(review.getTeacher()) : null;

        Integer subjectId = (review.getSubject() != null) ? review.getSubject().getId() : null;
        String subjectName = (review.getSubject() != null) ? review.getSubject().getName() : null;

        return new ReviewDisplayDto(
                review.getId(),
                authorId,
                authorUsername,
                teacherDto,
                subjectId,
                subjectName,
                review.getDate(),
                review.getGrade(),
                review.getComment()
        );
    }
}
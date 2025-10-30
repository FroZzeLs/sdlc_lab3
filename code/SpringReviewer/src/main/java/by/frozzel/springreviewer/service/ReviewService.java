package by.frozzel.springreviewer.service;

import by.frozzel.springreviewer.dto.ReviewCreateDto;
import by.frozzel.springreviewer.dto.ReviewDisplayDto;
import by.frozzel.springreviewer.exception.BadRequestException;
import by.frozzel.springreviewer.exception.ResourceNotFoundException;
import by.frozzel.springreviewer.mapper.ReviewMapper;
import by.frozzel.springreviewer.model.Review;
import by.frozzel.springreviewer.model.Subject;
import by.frozzel.springreviewer.model.Teacher;
import by.frozzel.springreviewer.model.User;
import by.frozzel.springreviewer.repository.ReviewRepository;
import by.frozzel.springreviewer.repository.SubjectRepository;
import by.frozzel.springreviewer.repository.TeacherRepository;
import by.frozzel.springreviewer.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;
    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

    private static final String USER_RESOURCE = "User";
    private static final String TEACHER_RESOURCE = "Teacher";
    private static final String SUBJECT_RESOURCE = "Subject";
    private static final String REVIEW_RESOURCE = "Review";
    private static final String ID_FIELD = "id";

    @Transactional
    public ReviewDisplayDto saveReview(ReviewCreateDto dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(USER_RESOURCE, ID_FIELD, dto.getUserId()));
        Teacher teacher = teacherRepository.findById(dto.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException(TEACHER_RESOURCE, ID_FIELD, dto.getTeacherId()));
        Subject subject = subjectRepository.findById(dto.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException(SUBJECT_RESOURCE, ID_FIELD, dto.getSubjectId()));

        boolean isTeaching = teacher.getSubjects().stream()
                .anyMatch(s -> s.getId() == dto.getSubjectId());

        if (!isTeaching) {
            throw new BadRequestException(String.format("Teacher %d does not teach subject %d", teacher.getId(), subject.getId()));
        }

        Review review = reviewMapper.toEntity(dto);
        review.setUser(user);
        review.setTeacher(teacher);
        review.setSubject(subject);
        review.setDate(Objects.requireNonNullElseGet(dto.getDate(), LocalDate::now));

        Review savedReview = reviewRepository.save(review);
        return reviewMapper.toDto(savedReview);
    }

    @Transactional(readOnly = true)
    public List<ReviewDisplayDto> getAllReviews() {
        return reviewRepository.findAll().stream()
                .map(reviewMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReviewDisplayDto getReviewById(Integer id) {
        return reviewRepository.findById(id)
                .map(reviewMapper::toDto)
                .orElseThrow(() -> {
                    log.warn("Review not found  with id: {}", id);
                    return new ResourceNotFoundException(REVIEW_RESOURCE, ID_FIELD, id);
                });
    }

    @Transactional
    public void deleteReview(Integer id) {
        reviewRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Review  not found with id: {}", id);
                    return new ResourceNotFoundException(REVIEW_RESOURCE, ID_FIELD, id);
                });

        reviewRepository.deleteById(id);
        log.debug("Deleted review with id: {}", id);
    }

    @Transactional
    public ReviewDisplayDto updateReview(Integer id, ReviewCreateDto dto) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn(" Review not found with id: {}", id);
                    return new ResourceNotFoundException(REVIEW_RESOURCE, ID_FIELD, id);
                });

        boolean updated = false;
        if (dto.getDate() != null && !dto.getDate().equals(review.getDate())) {
            review.setDate(dto.getDate());
            updated = true;
        }
        if (dto.getGrade() != null && !dto.getGrade().equals(review.getGrade())) {
            review.setGrade(dto.getGrade());
            updated = true;
        }
        if (dto.getComment() != null && !dto.getComment().equals(review.getComment())) {
            review.setComment(dto.getComment());
            updated = true;
        }

        if (updated) {
            Review updatedReview = reviewRepository.save(review);
            return reviewMapper.toDto(updatedReview);
        } else {
            log.debug("Review with id {} was not modified.", id);
            return reviewMapper.toDto(review);
        }
    }

    @Transactional(readOnly = true)
    public List<ReviewDisplayDto> getReviewsByTeacherId(Integer teacherId) {
        List<Review> reviews = reviewRepository.findByTeacherId(teacherId);
        if (reviews.isEmpty()) {
            log.warn("No reviews found for teacher ID: {}", teacherId);
            throw new ResourceNotFoundException("No reviews found for teacher ID: " + teacherId);
        }
        return reviews.stream().map(reviewMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewDisplayDto> getReviewsByUserId(Integer userId) {
        List<Review> reviews = reviewRepository.findByUserId(userId);
        if (reviews.isEmpty()) {
            log.warn("No reviews found for user ID: {}", userId);
            throw new ResourceNotFoundException("No reviews found for user ID: " + userId);
        }
        return reviews.stream().map(reviewMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewDisplayDto> getReviewsByUserUsername(String username) {
        List<Review> reviews = reviewRepository.findByUserUsernameIgnoreCase(username);
        if (reviews.isEmpty()) {
            log.warn("No reviews found for username: {}", username);
            throw new ResourceNotFoundException("No reviews found for username: " + username);
        }
        return reviews.stream().map(reviewMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<Object[]> getReviewCountsPerTeacher() {
        return reviewRepository.countReviewsPerTeacher();
    }

    @Transactional(readOnly = true)
    public List<ReviewDisplayDto> searchReviews(LocalDate startDate,
                                                LocalDate endDate, String teacherSurname,
                                                String subjectName, Integer minGrade) {
        log.info("Searching reviews directly from DB with criteria: startDate={}, endDate={}, teacherSurname='{}', subjectName='{}', minGrade={}",
                startDate, endDate, teacherSurname, subjectName, minGrade);

        List<Review> reviews = reviewRepository.searchReviews(startDate, endDate, teacherSurname, subjectName, minGrade);

        if (reviews.isEmpty()) {
            log.warn("No reviews found matching the specified search criteria.");
            throw new ResourceNotFoundException("No reviews found matching the specified criteria.");
        }

        return reviews.stream().map(reviewMapper::toDto).toList();
    }
}

package by.frozzel.springreviewer.repository;

import by.frozzel.springreviewer.model.Review;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Integer> {

    List<Review> findByTeacherId(Integer teacherId);

    List<Review> findByUserId(Integer userId);

    List<Review> findByUserUsernameIgnoreCase(String username);

    @Transactional
    @Modifying
    @Query("DELETE FROM Review r WHERE r.subject.id = :subjectId")
    void deleteBySubjectId(@Param("subjectId") int subjectId);

    @Query("SELECT t.surname, COUNT(r) "
            + "FROM Review r JOIN r.teacher t "
            + "GROUP BY t.id, t.surname "
            + "ORDER BY COUNT(r) DESC")
    List<Object[]> countReviewsPerTeacher();

    @Query(value = """
            SELECT r.* FROM reviews r
            JOIN teachers t ON r.teacher_id = t.id
            JOIN subjects s ON r.subject_id = s.id
            WHERE
                (CAST(:startDate AS DATE) IS NULL OR r.date >= CAST(:startDate AS DATE)) AND
                (CAST(:endDate AS DATE) IS NULL OR r.date <= CAST(:endDate AS DATE)) AND
                (CAST(:teacherSurname AS VARCHAR) IS NULL OR
                LOWER(t.surname) LIKE LOWER(CONCAT('%', CAST(:teacherSurname AS VARCHAR), '%'))) AND
                (CAST(:subjectName AS VARCHAR) IS NULL OR
                LOWER(s.name) LIKE LOWER(CONCAT('%', CAST(:subjectName AS VARCHAR), '%'))) AND
                (CAST(:minGrade AS INTEGER) IS NULL OR r.grade >= CAST(:minGrade AS INTEGER))
            """, nativeQuery = true)
    List<Review> searchReviews(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("teacherSurname") String teacherSurname,
            @Param("subjectName") String subjectName,
            @Param("minGrade") Integer minGrade
    );
}
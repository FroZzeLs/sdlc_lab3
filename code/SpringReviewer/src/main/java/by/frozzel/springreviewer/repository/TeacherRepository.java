package by.frozzel.springreviewer.repository;

import by.frozzel.springreviewer.model.Teacher;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface TeacherRepository extends JpaRepository<Teacher, Integer> {
    Optional<Teacher> findBySurnameAndNameIgnoreCase(String surname, String name);

    @Query("SELECT DISTINCT t FROM Teacher t JOIN t.subjects s WHERE"
           + " LOWER(s.name) = LOWER(:subjectName)")
    List<Teacher> findTeachersBySubjectName(@Param("subjectName") String subjectName);
}
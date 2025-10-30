package by.frozzel.springreviewer.repository;

import by.frozzel.springreviewer.model.Subject;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Integer> {
    Optional<Subject> findByNameIgnoreCase(String name);
}

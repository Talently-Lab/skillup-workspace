package skillup_workspace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import skillup_workspace.entity.Instructor;

@Repository
public interface InstructorRepository extends JpaRepository<Instructor, Integer> {
}
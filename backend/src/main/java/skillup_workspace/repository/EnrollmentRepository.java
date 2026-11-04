package skillup_workspace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import skillup_workspace.entity.Course;
import skillup_workspace.entity.Enrollment;
import skillup_workspace.entity.User;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    boolean existsByUserAndCourse(User user, Course course);
    boolean existsByCourse_Id(Long courseId);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE UPPER(e.user.role) = 'ALUMNO'")
    long countByStudentRole();
}
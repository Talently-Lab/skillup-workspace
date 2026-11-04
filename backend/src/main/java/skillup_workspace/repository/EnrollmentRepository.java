package skillup_workspace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import skillup_workspace.entity.Course;
import skillup_workspace.entity.Enrollment;
import skillup_workspace.entity.User;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    boolean existsByUserAndCourse(User user, Course course);
    boolean existsByCourse_Id(Long courseId);

    @Query("""
            SELECT e
            FROM Enrollment e
            JOIN FETCH e.course
            WHERE e.user = :user
            ORDER BY e.enrolledAt DESC
            """)
    List<Enrollment> findByUserWithCourseOrderByEnrolledAtDesc(@Param("user") User user);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE UPPER(e.user.role) = 'ALUMNO'")
    long countByStudentRole();

    @Query("""
            SELECT e.course.id AS courseId, COUNT(e.id) AS activeEnrollmentCount
            FROM Enrollment e
            WHERE UPPER(e.status) = 'ACTIVE'
            GROUP BY e.course.id
            """)
    List<CourseEnrollmentCount> findActiveEnrollmentCountsByCourse();

    interface CourseEnrollmentCount {
        Long getCourseId();
        Long getActiveEnrollmentCount();
    }
}
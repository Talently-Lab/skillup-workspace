package skillup_workspace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import skillup_workspace.entity.Course;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByIsActiveTrue();
    @Query("SELECT c FROM Course c WHERE :includeInactive = true OR c.isActive = true")
    List<Course> findAllCoursesWithFilter(@Param("includeInactive") boolean includeInactive);
}
package skillup_workspace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import skillup_workspace.entity.CourseFavorite;

@Repository
public interface CourseFavoriteRepository extends JpaRepository<CourseFavorite, Long> {

    boolean existsByUser_IdAndCourse_Id(Long userId, Long courseId);

    void deleteByUser_IdAndCourse_Id(Long userId, Long courseId);

    void deleteByCourse_Id(Long courseId);
}

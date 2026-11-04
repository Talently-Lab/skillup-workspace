package skillup_workspace.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skillup_workspace.dto.response.CourseFavoriteResponseDTO;
import skillup_workspace.entity.Course;
import skillup_workspace.entity.CourseFavorite;
import skillup_workspace.entity.User;
import skillup_workspace.exception.ResourceNotFoundException;
import skillup_workspace.repository.CourseFavoriteRepository;
import skillup_workspace.repository.CourseRepository;
import skillup_workspace.repository.UserRepository;
import skillup_workspace.service.CourseFavoriteService;

@Service
@RequiredArgsConstructor
public class CourseFavoriteServiceImpl implements CourseFavoriteService {

    private final CourseFavoriteRepository favoriteRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public CourseFavoriteResponseDTO getFavoriteStatus(Long courseId) {
        User user = getCurrentUser();
        getCourse(courseId);
        return new CourseFavoriteResponseDTO(
                courseId,
                favoriteRepository.existsByUser_IdAndCourse_Id(user.getId(), courseId)
        );
    }

    @Override
    @Transactional
    public void addFavorite(Long courseId) {
        User user = getCurrentUser();
        Course course = getCourse(courseId);
        if (favoriteRepository.existsByUser_IdAndCourse_Id(user.getId(), courseId)) {
            return;
        }

        CourseFavorite favorite = new CourseFavorite();
        favorite.setUser(user);
        favorite.setCourse(course);
        favoriteRepository.save(favorite);
    }

    @Override
    @Transactional
    public void removeFavorite(Long courseId) {
        User user = getCurrentUser();
        getCourse(courseId);
        favoriteRepository.deleteByUser_IdAndCourse_Id(user.getId(), courseId);
    }

    private Course getCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Curso no encontrado con el id: " + courseId
                ));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getPrincipal() instanceof User user
                ? user.getEmail()
                : authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }
}

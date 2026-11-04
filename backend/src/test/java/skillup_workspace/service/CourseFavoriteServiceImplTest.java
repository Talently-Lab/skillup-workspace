package skillup_workspace.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import skillup_workspace.dto.response.CourseFavoriteResponseDTO;
import skillup_workspace.entity.Course;
import skillup_workspace.entity.CourseFavorite;
import skillup_workspace.entity.User;
import skillup_workspace.exception.ResourceNotFoundException;
import skillup_workspace.repository.CourseFavoriteRepository;
import skillup_workspace.repository.CourseRepository;
import skillup_workspace.repository.UserRepository;
import skillup_workspace.service.impl.CourseFavoriteServiceImpl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseFavoriteServiceImplTest {

    @Mock
    private CourseFavoriteRepository favoriteRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CourseFavoriteServiceImpl favoriteService;

    private User student;
    private Course course;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(7L);
        student.setEmail("student@example.com");
        course = new Course();
        course.setId(12L);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(student, null)
        );
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getFavoriteStatusReturnsPersistedState() {
        when(courseRepository.findById(12L)).thenReturn(Optional.of(course));
        when(favoriteRepository.existsByUser_IdAndCourse_Id(7L, 12L)).thenReturn(true);

        CourseFavoriteResponseDTO result = favoriteService.getFavoriteStatus(12L);

        assertEquals(12L, result.getCourseId());
        assertTrue(result.isFavorited());
    }

    @Test
    void addFavoriteIsIdempotentWhenAlreadyPresent() {
        when(courseRepository.findById(12L)).thenReturn(Optional.of(course));
        when(favoriteRepository.existsByUser_IdAndCourse_Id(7L, 12L)).thenReturn(true);

        favoriteService.addFavorite(12L);

        verify(favoriteRepository, never()).save(any(CourseFavorite.class));
    }

    @Test
    void addFavoritePersistsNewRelation() {
        when(courseRepository.findById(12L)).thenReturn(Optional.of(course));
        when(favoriteRepository.existsByUser_IdAndCourse_Id(7L, 12L)).thenReturn(false);

        favoriteService.addFavorite(12L);

        verify(favoriteRepository).save(argThat(favorite ->
                favorite.getUser() == student && favorite.getCourse() == course
        ));
    }

    @Test
    void removeFavoriteDeletesRelationIdempotently() {
        when(courseRepository.findById(12L)).thenReturn(Optional.of(course));
        favoriteService.removeFavorite(12L);
        favoriteService.removeFavorite(12L);

        verify(favoriteRepository, times(2)).deleteByUser_IdAndCourse_Id(7L, 12L);
    }

    @Test
    void favoriteOperationsRejectUnknownCourse() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> favoriteService.addFavorite(99L));
        verify(favoriteRepository, never()).save(any(CourseFavorite.class));
    }
}

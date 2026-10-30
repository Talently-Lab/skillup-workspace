package skillup_workspace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import skillup_workspace.dto.request.EnrollmentRequestDTO;
import skillup_workspace.dto.response.EnrollmentResponseDTO;
import skillup_workspace.entity.Course;
import skillup_workspace.entity.Enrollment;
import skillup_workspace.entity.User;
import skillup_workspace.repository.CourseRepository;
import skillup_workspace.repository.EnrollmentRepository;
import skillup_workspace.repository.UserRepository;
import skillup_workspace.service.impl.EnrollmentServiceImpl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceImplTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private EnrollmentServiceImpl enrollmentService;

    private User mockUser;
    private Course mockCourse;
    private EnrollmentRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("alumno@ejemplo.com");

        mockCourse = new Course();
        mockCourse.setId(1L);
        mockCourse.setTitle("Curso de Prueba");
        mockCourse.setIsActive(true);

        requestDTO = new EnrollmentRequestDTO();
        requestDTO.setCourseId(1L);

        SecurityContext securityContext = mock(SecurityContext.class);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(mockUser, null, null);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void enrollUser_Success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(courseRepository.findById(anyLong())).thenReturn(Optional.of(mockCourse));
        when(enrollmentRepository.existsByUserAndCourse(any(User.class), any(Course.class))).thenReturn(false);

        Enrollment mockEnrollment = new Enrollment();
        mockEnrollment.setId(100L);
        mockEnrollment.setStatus("ACTIVE");
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(mockEnrollment);

        EnrollmentResponseDTO response = enrollmentService.enrollUser(requestDTO);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(1L, response.getCourseId());
        verify(enrollmentRepository, times(1)).save(any(Enrollment.class));
    }

    @Test
    void enrollUser_CourseInactive_ThrowsException() {
        mockCourse.setIsActive(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(courseRepository.findById(anyLong())).thenReturn(Optional.of(mockCourse));

        Exception exception = assertThrows(IllegalStateException.class, () -> enrollmentService.enrollUser(requestDTO));
        assertEquals("No puedes inscribirte a un curso inactivo.", exception.getMessage());
    }

    @Test
    void enrollUser_AlreadyEnrolled_ThrowsException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(courseRepository.findById(anyLong())).thenReturn(Optional.of(mockCourse));
        when(enrollmentRepository.existsByUserAndCourse(any(User.class), any(Course.class))).thenReturn(true);

        Exception exception = assertThrows(IllegalStateException.class, () -> enrollmentService.enrollUser(requestDTO));
        assertEquals("El usuario ya se encuentra inscripto en este curso.", exception.getMessage());
    }
}
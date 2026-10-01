package skillup_workspace.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import skillup_workspace.dto.request.CourseRequestDTO;
import skillup_workspace.dto.response.CourseDetailResponseDTO;
import skillup_workspace.dto.response.CourseResponseDTO;
import skillup_workspace.entity.Course;
import skillup_workspace.exception.ResourceNotFoundException;
import skillup_workspace.mapper.CourseMapper;
import skillup_workspace.repository.CourseRepository;
import skillup_workspace.service.impl.CourseServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseMapper courseMapper;

    @InjectMocks
    private CourseServiceImpl courseService;

    private Course mockCourse;
    private CourseRequestDTO requestDTO;
    private CourseDetailResponseDTO detailResponseDTO;
    private CourseResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        mockCourse = new Course();
        mockCourse.setId(1L);
        mockCourse.setTitle("Curso de Java");
        mockCourse.setIsActive(true);

        requestDTO = new CourseRequestDTO();
        detailResponseDTO = new CourseDetailResponseDTO();
        responseDTO = new CourseResponseDTO();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setupAuthentication(String role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user",
                "password",
                List.of(new SimpleGrantedAuthority(role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }


    @Nested
    @DisplayName("Pruebas para getAllCourses")
    class GetAllCoursesTests {

        @Test
        @DisplayName("getAllCourses - Debería filtrar como ADMIN cuando el usuario tiene ROLE_ADMIN")
        void getAllCourses_WhenUserIsAdmin_ShouldPassTrueToRepository() {
            setupAuthentication("ROLE_ADMIN");
            given(courseRepository.findAllCoursesWithFilter(true)).willReturn(List.of(mockCourse));
            given(courseMapper.toResponseDTOList(anyList())).willReturn(List.of(responseDTO));


            List<CourseResponseDTO> result = courseService.getAllCourses();


            assertNotNull(result);
            assertEquals(1, result.size());
            verify(courseRepository, times(1)).findAllCoursesWithFilter(true);
        }

        @Test
        @DisplayName("getAllCourses - Debería filtrar como NO ADMIN cuando el usuario es ALUMNO")
        void getAllCourses_WhenUserIsStudent_ShouldPassFalseToRepository() {

            setupAuthentication("ROLE_ALUMNO");
            given(courseRepository.findAllCoursesWithFilter(false)).willReturn(List.of(mockCourse));
            given(courseMapper.toResponseDTOList(anyList())).willReturn(List.of(responseDTO));


            List<CourseResponseDTO> result = courseService.getAllCourses();


            assertNotNull(result);
            assertEquals(1, result.size());
            verify(courseRepository, times(1)).findAllCoursesWithFilter(false);
        }

        @Test
        @DisplayName("getAllCourses - Debería retornar false en checkIsAdmin cuando SecurityContext no tiene autenticación")
        void getAllCourses_WhenAuthenticationIsNull_ShouldPassFalseToRepository() {

            SecurityContextHolder.clearContext();
            given(courseRepository.findAllCoursesWithFilter(false)).willReturn(List.of(mockCourse));
            given(courseMapper.toResponseDTOList(anyList())).willReturn(List.of(responseDTO));


            List<CourseResponseDTO> result = courseService.getAllCourses();


            assertNotNull(result);
            verify(courseRepository, times(1)).findAllCoursesWithFilter(false);
        }

        @Test
        @DisplayName("getAllCourses - Debería retornar false en checkIsAdmin cuando el usuario no está autenticado")
        void getAllCourses_WhenUserIsNotAuthenticated_ShouldPassFalseToRepository() {

            UsernamePasswordAuthenticationToken unauthenticatedToken =
                    new UsernamePasswordAuthenticationToken("user", "password");
            unauthenticatedToken.setAuthenticated(false);
            SecurityContextHolder.getContext().setAuthentication(unauthenticatedToken);

            given(courseRepository.findAllCoursesWithFilter(false)).willReturn(List.of(mockCourse));
            given(courseMapper.toResponseDTOList(anyList())).willReturn(List.of(responseDTO));


            List<CourseResponseDTO> result = courseService.getAllCourses();


            assertNotNull(result);
            verify(courseRepository, times(1)).findAllCoursesWithFilter(false);
        }
    }


    @Nested
    @DisplayName("Pruebas para getCourseById")
    class GetCourseByIdTests {

        @Test
        @DisplayName("getCourseById - Debería retornar el detalle si el curso existe y está activo")
        void getCourseById_WhenCourseExistsAndIsActive_ShouldReturnDetail() {

            given(courseRepository.findById(1L)).willReturn(Optional.of(mockCourse));
            given(courseMapper.toDetailResponseDTO(mockCourse)).willReturn(detailResponseDTO);


            CourseDetailResponseDTO result = courseService.getCourseById(1L);


            assertNotNull(result);
            verify(courseRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("getCourseById - Debería lanzar ResourceNotFoundException si el curso no existe")
        void getCourseById_WhenCourseDoesNotExist_ShouldThrowException() {

            given(courseRepository.findById(1L)).willReturn(Optional.empty());


            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> courseService.getCourseById(1L)
            );

            assertEquals("Curso no encontrado con el id: 1", exception.getMessage());
        }

        @Test
        @DisplayName("getCourseById - Debería lanzar ResourceNotFoundException si el curso está inactivo y el usuario NO es ADMIN")
        void getCourseById_WhenCourseIsInactiveAndUserIsNotAdmin_ShouldThrowException() {

            mockCourse.setIsActive(false);
            setupAuthentication("ROLE_ALUMNO");
            given(courseRepository.findById(1L)).willReturn(Optional.of(mockCourse));


            assertThrows(
                    ResourceNotFoundException.class,
                    () -> courseService.getCourseById(1L)
            );
        }

        @Test
        @DisplayName("getCourseById - Debería retornar el detalle si el curso está inactivo pero el usuario ES ADMIN")
        void getCourseById_WhenCourseIsInactiveAndUserIsAdmin_ShouldReturnDetail() {

            mockCourse.setIsActive(false);
            setupAuthentication("ROLE_ADMIN");
            given(courseRepository.findById(1L)).willReturn(Optional.of(mockCourse));
            given(courseMapper.toDetailResponseDTO(mockCourse)).willReturn(detailResponseDTO);


            CourseDetailResponseDTO result = courseService.getCourseById(1L);


            assertNotNull(result);
            verify(courseRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("getCourseById - Debería lanzar ResourceNotFoundException si getIsActive es null y el usuario NO es ADMIN")
        void getCourseById_WhenCourseIsActiveIsNullAndUserIsNotAdmin_ShouldThrowException() {

            mockCourse.setIsActive(null);
            setupAuthentication("ROLE_ALUMNO");
            given(courseRepository.findById(1L)).willReturn(Optional.of(mockCourse));


            assertThrows(
                    ResourceNotFoundException.class,
                    () -> courseService.getCourseById(1L)
            );
        }

        @Test
        @DisplayName("getCourseById - Debería retornar el detalle si getIsActive es null pero el usuario ES ADMIN")
        void getCourseById_WhenCourseIsActiveIsNullAndUserIsAdmin_ShouldReturnDetail() {

            mockCourse.setIsActive(null);
            setupAuthentication("ROLE_ADMIN");
            given(courseRepository.findById(1L)).willReturn(Optional.of(mockCourse));
            given(courseMapper.toDetailResponseDTO(mockCourse)).willReturn(detailResponseDTO);


            CourseDetailResponseDTO result = courseService.getCourseById(1L);


            assertNotNull(result);
            verify(courseRepository, times(1)).findById(1L);
        }
    }



    @Test
    @DisplayName("createCourse - Debería mapear, guardar y retornar el nuevo curso")
    void createCourse_ShouldSaveAndReturnCourse() {

        given(courseMapper.toEntity(requestDTO)).willReturn(mockCourse);
        given(courseRepository.save(mockCourse)).willReturn(mockCourse);
        given(courseMapper.toDetailResponseDTO(mockCourse)).willReturn(detailResponseDTO);


        CourseDetailResponseDTO result = courseService.createCourse(requestDTO);


        assertNotNull(result);
        verify(courseMapper, times(1)).toEntity(requestDTO);
        verify(courseRepository, times(1)).save(mockCourse);
    }


    @Nested
    @DisplayName("Pruebas para updateCourse")
    class UpdateCourseTests {

        @Test
        @DisplayName("updateCourse - Debería actualizar un curso existente exitosamente")
        void updateCourse_WhenCourseExists_ShouldUpdateAndReturn() {

            given(courseRepository.findById(1L)).willReturn(Optional.of(mockCourse));
            doNothing().when(courseMapper).updateEntityFromDto(requestDTO, mockCourse);
            given(courseRepository.save(mockCourse)).willReturn(mockCourse);
            given(courseMapper.toDetailResponseDTO(mockCourse)).willReturn(detailResponseDTO);

            CourseDetailResponseDTO result = courseService.updateCourse(1L, requestDTO);

            assertNotNull(result);
            verify(courseMapper, times(1)).updateEntityFromDto(requestDTO, mockCourse);
            verify(courseRepository, times(1)).save(mockCourse);
        }

        @Test
        @DisplayName("updateCourse - Debería lanzar ResourceNotFoundException cuando el curso no existe")
        void updateCourse_WhenCourseDoesNotExist_ShouldThrowException() {
            given(courseRepository.findById(1L)).willReturn(Optional.empty());

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> courseService.updateCourse(1L, requestDTO)
            );

            verify(courseRepository, never()).save(any());
        }
    }


    @Nested
    @DisplayName("Pruebas para deleteCourse")
    class DeleteCourseTests {

        @Test
        @DisplayName("deleteCourse - Debería realizar un borrado lógico marcando isActive = false")
        void deleteCourse_WhenCourseExists_ShouldSetIsActiveToFalse() {

            given(courseRepository.findById(1L)).willReturn(Optional.of(mockCourse));
            given(courseRepository.save(mockCourse)).willReturn(mockCourse);

            courseService.deleteCourse(1L);

            assertFalse(mockCourse.getIsActive());
            verify(courseRepository, times(1)).save(mockCourse);
        }

        @Test
        @DisplayName("deleteCourse - Debería lanzar ResourceNotFoundException si el curso a eliminar no existe")
        void deleteCourse_WhenCourseDoesNotExist_ShouldThrowException() {

            given(courseRepository.findById(1L)).willReturn(Optional.empty());

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> courseService.deleteCourse(1L)
            );

            verify(courseRepository, never()).save(any());
        }
    }
}
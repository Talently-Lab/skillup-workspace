package skillup_workspace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import skillup_workspace.dto.request.CourseRequestDTO;
import skillup_workspace.dto.response.CourseDetailResponseDTO;
import skillup_workspace.dto.response.CourseResponseDTO;
import skillup_workspace.entity.Course;
import skillup_workspace.entity.Instructor;
import skillup_workspace.exception.ResourceNotFoundException;
import skillup_workspace.mapper.CourseMapper;
import skillup_workspace.repository.CourseRepository;
import skillup_workspace.repository.InstructorRepository;
import skillup_workspace.service.impl.CourseServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private InstructorRepository instructorRepository;

    @Mock
    private CourseMapper courseMapper;

    @InjectMocks
    private CourseServiceImpl courseService;

    private Course course;
    private Instructor instructor;
    private CourseResponseDTO courseResponseDTO;
    private CourseDetailResponseDTO courseDetailResponseDTO;
    private CourseRequestDTO courseRequestDTO;

    @BeforeEach
    void setUp() {
        instructor = new Instructor();
        instructor.setInstructorId(1);
        instructor.setName("Carlos Perez");

        course = new Course();
        course.setCourseId(1);
        course.setTitle("Diseño UX/UI");
        course.setActive(true);

        courseResponseDTO = new CourseResponseDTO();
        courseResponseDTO.setId(1);
        courseResponseDTO.setTitle("Diseño UX/UI");

        courseDetailResponseDTO = new CourseDetailResponseDTO();
        courseDetailResponseDTO.setId(1);
        courseDetailResponseDTO.setTitle("Diseño UX/UI");

        courseRequestDTO = new CourseRequestDTO();
        courseRequestDTO.setTitle("Diseño UX/UI Actualizado");
    }

    @Test
    @DisplayName("Debe retornar la lista de cursos activos")
    void getActiveCourses_ShouldReturnListOfCourses() {
        when(courseRepository.findByActiveTrue()).thenReturn(List.of(course));
        when(courseMapper.toResponseDTO(course)).thenReturn(courseResponseDTO);

        List<CourseResponseDTO> result = courseService.getActiveCourses();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Diseño UX/UI", result.get(0).getTitle());
        verify(courseRepository, times(1)).findByActiveTrue();
        verify(courseMapper, times(1)).toResponseDTO(course);
    }

    @Test
    @DisplayName("Debe retornar el detalle de un curso existente por ID")
    void getCourseById_WhenCourseExists_ShouldReturnCourseDetail() {
        when(courseRepository.findById(1)).thenReturn(Optional.of(course));
        when(courseMapper.toDetailResponseDTO(course)).thenReturn(courseDetailResponseDTO);

        CourseDetailResponseDTO result = courseService.getCourseById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        verify(courseRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException cuando el curso no existe por ID")
    void getCourseById_WhenCourseDoesNotExist_ShouldThrowException() {
        when(courseRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> courseService.getCourseById(99));
        verify(courseRepository, times(1)).findById(99);
    }

    @Test
    @DisplayName("Debe crear un curso exitosamente sin instructor")
    void createCourse_WithoutInstructor_ShouldReturnCreatedCourseDetail() {
        when(courseMapper.toEntity(courseRequestDTO)).thenReturn(course);
        when(courseRepository.save(any(Course.class))).thenReturn(course);
        when(courseMapper.toDetailResponseDTO(course)).thenReturn(courseDetailResponseDTO);

        CourseDetailResponseDTO result = courseService.createCourse(courseRequestDTO);

        assertNotNull(result);
        assertEquals("Diseño UX/UI", result.getTitle());
        verify(instructorRepository, never()).save(any());
        verify(courseRepository, times(1)).save(any(Course.class));
    }

    @Test
    @DisplayName("Debe crear un curso guardando también el instructor si está presente")
    void createCourse_WithInstructor_ShouldSaveInstructorAndCourse() {
        course.setInstructor(instructor);
        when(courseMapper.toEntity(courseRequestDTO)).thenReturn(course);
        when(instructorRepository.save(instructor)).thenReturn(instructor);
        when(courseRepository.save(any(Course.class))).thenReturn(course);
        when(courseMapper.toDetailResponseDTO(course)).thenReturn(courseDetailResponseDTO);

        CourseDetailResponseDTO result = courseService.createCourse(courseRequestDTO);

        assertNotNull(result);
        verify(instructorRepository, times(1)).save(instructor);
        verify(courseRepository, times(1)).save(course);
    }

    @Test
    @DisplayName("Debe actualizar un curso existente correctamente con instructor")
    void updateCourse_WhenCourseExists_WithInstructor_ShouldReturnUpdatedCourseDetail() {
        course.setInstructor(instructor);
        when(courseRepository.findById(1)).thenReturn(Optional.of(course));
        when(instructorRepository.save(instructor)).thenReturn(instructor);
        when(courseRepository.save(any(Course.class))).thenReturn(course);
        when(courseMapper.toDetailResponseDTO(course)).thenReturn(courseDetailResponseDTO);

        CourseDetailResponseDTO result = courseService.updateCourse(1, courseRequestDTO);

        assertNotNull(result);
        verify(courseRepository, times(1)).findById(1);
        verify(courseMapper, times(1)).updateEntityFromDto(courseRequestDTO, course);
        verify(instructorRepository, times(1)).save(instructor);
        verify(courseRepository, times(1)).save(course);
    }

    @Test
    @DisplayName("Debe actualizar un curso existente correctamente sin instructor")
    void updateCourse_WhenCourseExists_WithoutInstructor_ShouldReturnUpdatedCourseDetail() {
        // course NO tiene instructor (instructor es null)
        when(courseRepository.findById(1)).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenReturn(course);
        when(courseMapper.toDetailResponseDTO(course)).thenReturn(courseDetailResponseDTO);

        CourseDetailResponseDTO result = courseService.updateCourse(1, courseRequestDTO);

        assertNotNull(result);
        verify(courseRepository, times(1)).findById(1);
        verify(courseMapper, times(1)).updateEntityFromDto(courseRequestDTO, course);
        verify(instructorRepository, never()).save(any()); // Cubre la rama cuando instructor es null
        verify(courseRepository, times(1)).save(course);
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException al intentar actualizar un curso inexistente")
    void updateCourse_WhenCourseDoesNotExist_ShouldThrowException() {
        when(courseRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> courseService.updateCourse(99, courseRequestDTO));
        verify(courseRepository, times(1)).findById(99);
        verify(courseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe desactivar (borrado lógico) un curso existente")
    void deleteCourse_WhenCourseExists_ShouldSetActiveToFalse() {
        when(courseRepository.findById(1)).thenReturn(Optional.of(course));

        courseService.deleteCourse(1);

        assertFalse(course.getActive());
        verify(courseRepository, times(1)).save(course);
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException al intentar eliminar un curso inexistente")
    void deleteCourse_WhenDoesNotExist_ShouldThrowException() {
        when(courseRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> courseService.deleteCourse(99));
        verify(courseRepository, times(1)).findById(99);
        verify(courseRepository, never()).save(any());
    }
}
package skillup_workspace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import skillup_workspace.dto.request.CourseRequestDTO;
import skillup_workspace.dto.response.CourseDetailResponseDTO;
import skillup_workspace.dto.response.CourseResponseDTO;
import skillup_workspace.exception.ExceptionConstants;
import skillup_workspace.exception.ResourceNotFoundException;
import skillup_workspace.service.CourseService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CourseController.class)
@AutoConfigureMockMvc(addFilters = false)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CourseService courseService;

    private CourseResponseDTO courseResponseDTO;
    private CourseDetailResponseDTO courseDetailResponseDTO;
    private CourseRequestDTO courseRequestDTO;

    @BeforeEach
    void setUp() {
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
    @DisplayName("GET /api/v1/courses - Debe retornar 200 OK y la lista de cursos activos")
    void getActiveCourses_ShouldReturn200AndListOfCourses() throws Exception {
        when(courseService.getActiveCourses()).thenReturn(List.of(courseResponseDTO));

        mockMvc.perform(get("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Diseño UX/UI"));

        verify(courseService, times(1)).getActiveCourses();
    }

    @Test
    @DisplayName("GET /api/v1/courses/{id} - Debe retornar 200 OK cuando el curso existe")
    void getCourseById_WhenExists_ShouldReturn200AndCourseDetail() throws Exception {
        when(courseService.getCourseById(1)).thenReturn(courseDetailResponseDTO);

        mockMvc.perform(get("/api/v1/courses/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Diseño UX/UI"));

        verify(courseService, times(1)).getCourseById(1);
    }

    @Test
    @DisplayName("GET /api/v1/courses/{id} - Debe retornar 404 Not Found cuando el curso no existe")
    void getCourseById_WhenDoesNotExist_ShouldReturn404() throws Exception {
        when(courseService.getCourseById(99))
                .thenThrow(new ResourceNotFoundException(String.format(ExceptionConstants.COURSE_NOT_FOUND, 99)));

        mockMvc.perform(get("/api/v1/courses/{id}", 99)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(courseService, times(1)).getCourseById(99);
    }

    @Test
    @DisplayName("POST /api/v1/courses - Debe retornar 201 Created al crear un curso")
    void createCourse_ShouldReturn201AndCreatedCourse() throws Exception {
        when(courseService.createCourse(any(CourseRequestDTO.class))).thenReturn(courseDetailResponseDTO);

        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(courseRequestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Diseño UX/UI"));

        verify(courseService, times(1)).createCourse(any(CourseRequestDTO.class));
    }

    @Test
    @DisplayName("PUT /api/v1/courses/{id} - Debe retornar 200 OK al actualizar un curso")
    void updateCourse_WhenExists_ShouldReturn200AndUpdatedCourse() throws Exception {
        when(courseService.updateCourse(eq(1), any(CourseRequestDTO.class))).thenReturn(courseDetailResponseDTO);

        mockMvc.perform(put("/api/v1/courses/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(courseRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Diseño UX/UI"));

        verify(courseService, times(1)).updateCourse(eq(1), any(CourseRequestDTO.class));
    }

    @Test
    @DisplayName("DELETE /api/v1/courses/{id} - Debe retornar 204 No Content al eliminar un curso")
    void deleteCourse_WhenExists_ShouldReturn204() throws Exception {
        doNothing().when(courseService).deleteCourse(1);

        mockMvc.perform(delete("/api/v1/courses/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(courseService, times(1)).deleteCourse(1);
    }

    @Test
    @DisplayName("DELETE /api/v1/courses/{id} - Debe retornar 404 Not Found al eliminar curso inexistente")
    void deleteCourse_WhenDoesNotExist_ShouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException(String.format(ExceptionConstants.COURSE_NOT_FOUND, 99)))
                .when(courseService).deleteCourse(99);

        mockMvc.perform(delete("/api/v1/courses/{id}", 99)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(courseService, times(1)).deleteCourse(99);
    }
}
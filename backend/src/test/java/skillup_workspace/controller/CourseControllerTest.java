package skillup_workspace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import skillup_workspace.dto.request.CourseRequestDTO;
import skillup_workspace.dto.response.CourseDetailResponseDTO;
import skillup_workspace.dto.response.CourseResponseDTO;
import skillup_workspace.service.CourseService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CourseControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CourseService courseService;

    @InjectMocks
    private CourseController courseController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(courseController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("GET /api/v1/courses - Debería retornar la lista de cursos con HttpStatus 200 OK")
    void getAllCourses_ShouldReturnListOfCourses() throws Exception {
        CourseResponseDTO courseResponse = new CourseResponseDTO();
        given(courseService.getAllCourses()).willReturn(List.of(courseResponse));

        mockMvc.perform(get("/api/v1/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/courses/{id} - Debería retornar el detalle del curso con HttpStatus 200 OK")
    void getCourseById_ShouldReturnCourseDetail() throws Exception {
        Long courseId = 1L;
        CourseDetailResponseDTO detailResponse = new CourseDetailResponseDTO();
        given(courseService.getCourseById(courseId)).willReturn(detailResponse);

        mockMvc.perform(get("/api/v1/courses/{id}", courseId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/courses - Debería crear un curso y retornar HttpStatus 201 CREATED")
    void createCourse_ShouldReturnCreatedCourse() throws Exception {
        CourseRequestDTO requestDTO = new CourseRequestDTO();
        CourseDetailResponseDTO createdResponse = new CourseDetailResponseDTO();

        given(courseService.createCourse(any(CourseRequestDTO.class))).willReturn(createdResponse);

        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("PUT /api/v1/courses/{id} - Debería actualizar un curso y retornar HttpStatus 200 OK")
    void updateCourse_ShouldReturnUpdatedCourse() throws Exception {
        Long courseId = 1L;
        CourseRequestDTO requestDTO = new CourseRequestDTO();
        CourseDetailResponseDTO updatedResponse = new CourseDetailResponseDTO();

        given(courseService.updateCourse(eq(courseId), any(CourseRequestDTO.class))).willReturn(updatedResponse);

        mockMvc.perform(put("/api/v1/courses/{id}", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/v1/courses/{id} - Debería eliminar un curso y retornar HttpStatus 204 NO_CONTENT")
    void deleteCourse_ShouldReturnNoContent() throws Exception {
        Long courseId = 1L;
        willDoNothing().given(courseService).deleteCourse(courseId);

        mockMvc.perform(delete("/api/v1/courses/{id}", courseId))
                .andExpect(status().isNoContent());
    }
}
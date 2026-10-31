package skillup_workspace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import skillup_workspace.config.JsonTypeValidationConfig;
import skillup_workspace.dto.request.CourseRequestDTO;
import skillup_workspace.dto.request.ModalityRequestDTO;
import skillup_workspace.dto.response.CourseDetailResponseDTO;
import skillup_workspace.dto.response.CourseResponseDTO;
import skillup_workspace.exception.GlobalExceptionHandler;
import skillup_workspace.exception.ResourceNotFoundException;
import skillup_workspace.service.CourseService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
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

    private CourseRequestDTO validCourseRequestDTO() {
        CourseRequestDTO requestDTO = new CourseRequestDTO();
        requestDTO.setTitle("Curso de Java");
        requestDTO.setSummary("Resumen del curso");
        requestDTO.setDescription("Descripción del curso");
        requestDTO.setCategory("Programación");
        requestDTO.setLevel("Principiante");
        requestDTO.setDurationHours(20);
        requestDTO.setDurationWeeks(4);
        requestDTO.setModality(new ModalityRequestDTO());
        return requestDTO;
    }

    @BeforeEach
    void setUp() {
        Jackson2ObjectMapperBuilder objectMapperBuilder = new Jackson2ObjectMapperBuilder();
        new JsonTypeValidationConfig().strictJsonTypeCoercion().customize(objectMapperBuilder);
        objectMapper = objectMapperBuilder.build();
        mockMvc = MockMvcBuilders.standaloneSetup(courseController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
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
    @DisplayName("GET /api/v1/courses/{id} - Debería retornar 400 si el ID no es numérico")
    void getCourseById_ShouldReturnBadRequestWhenIdIsNotNumeric() throws Exception {
        mockMvc.perform(get("/api/v1/courses/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("El parámetro 'id' tiene un formato inválido."));

        Mockito.verify(courseService, Mockito.never()).getCourseById(any());
    }

    @Test
    @DisplayName("POST /api/v1/courses - Debería crear un curso y retornar HttpStatus 201 CREATED")
    void createCourse_ShouldReturnCreatedCourse() throws Exception {
        CourseRequestDTO requestDTO = validCourseRequestDTO();
        CourseDetailResponseDTO createdResponse = new CourseDetailResponseDTO();

        given(courseService.createCourse(any(CourseRequestDTO.class))).willReturn(createdResponse);

        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /api/v1/courses - Debería retornar 400 para un valor numérico inválido")
    void createCourse_ShouldReturnBadRequestWhenNumericFieldHasInvalidType() throws Exception {
        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Diseño UX/UI",
                                  "summary": "Resumen",
                                  "description": "Descripción",
                                  "category": "Diseño",
                                  "level": "Intermedio",
                                  "durationHours": "cuarenta",
                                  "durationWeeks": 13,
                                  "modality": {}
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        "El JSON contiene valores inválidos o incompatibles con el tipo esperado."
                ));

        Mockito.verify(courseService, Mockito.never()).createCourse(any(CourseRequestDTO.class));
    }

    @Test
    @DisplayName("POST /api/v1/courses - Debería rechazar números para campos de texto")
    void createCourse_ShouldReturnBadRequestWhenTextFieldIsNumeric() throws Exception {
        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Diseño UX/UI",
                                  "summary": "Resumen",
                                  "description": "Descripción",
                                  "category": "Diseño",
                                  "level": "Intermedio",
                                  "durationHours": 40,
                                  "durationWeeks": 13,
                                  "schedule": {
                                    "days": 40,
                                    "startTime": "17:30",
                                    "endTime": "19:30"
                                  },
                                  "modality": {}
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        Mockito.verify(courseService, Mockito.never()).createCourse(any(CourseRequestDTO.class));
    }

    @Test
    @DisplayName("POST /api/v1/courses - Debería rechazar texto para campos numéricos")
    void createCourse_ShouldReturnBadRequestWhenNumericFieldIsString() throws Exception {
        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Diseño UX/UI",
                                  "summary": "Resumen",
                                  "description": "Descripción",
                                  "category": "Diseño",
                                  "level": "Intermedio",
                                  "durationHours": "40",
                                  "durationWeeks": 13,
                                  "modality": {}
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        Mockito.verify(courseService, Mockito.never()).createCourse(any(CourseRequestDTO.class));
    }

    @Test
    @DisplayName("POST /api/v1/courses - Debería rechazar nextStartDate numérica")
    void createCourse_ShouldReturnBadRequestWhenNextStartDateIsNumeric() throws Exception {
        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Diseño UX/UI",
                                  "summary": "Resumen",
                                  "description": "Descripción",
                                  "category": "Diseño",
                                  "level": "Intermedio",
                                  "durationHours": 40,
                                  "durationWeeks": 13,
                                  "nextStartDate": 2026,
                                  "modality": {}
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        Mockito.verify(courseService, Mockito.never()).createCourse(any(CourseRequestDTO.class));
    }

    @Test
    @DisplayName("POST /api/v1/courses - Debería aceptar nextStartDate en formato ISO")
    void createCourse_ShouldAcceptNextStartDateInIsoFormat() throws Exception {
        given(courseService.createCourse(any(CourseRequestDTO.class)))
                .willReturn(new CourseDetailResponseDTO());

        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Diseño UX/UI",
                                  "summary": "Resumen",
                                  "description": "Descripción",
                                  "category": "Diseño",
                                  "level": "Intermedio",
                                  "durationHours": 40,
                                  "durationWeeks": 13,
                                  "nextStartDate": "2026-11-07",
                                  "modality": {}
                                }
                                """))
                .andExpect(status().isCreated());

        Mockito.verify(courseService).createCourse(Mockito.argThat(request ->
                java.time.LocalDate.of(2026, 11, 7).equals(request.getNextStartDate())
        ));
    }

    @Test
    @DisplayName("PUT /api/v1/courses/{id} - Debería actualizar un curso y retornar HttpStatus 200 OK")
    void updateCourse_ShouldReturnUpdatedCourse() throws Exception {
        Long courseId = 1L;
        CourseRequestDTO requestDTO = validCourseRequestDTO();
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

    @Test
    @DisplayName("DELETE /api/v1/courses/{id} - Debería retornar 404 si el curso no existe")
    void deleteCourse_ShouldReturnNotFoundWhenCourseDoesNotExist() throws Exception {
        Long courseId = 999L;
        willThrow(new ResourceNotFoundException("Curso no encontrado con el id: " + courseId))
                .given(courseService).deleteCourse(courseId);

        mockMvc.perform(delete("/api/v1/courses/{id}", courseId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
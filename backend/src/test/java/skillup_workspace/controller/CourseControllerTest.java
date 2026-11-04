package skillup_workspace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import skillup_workspace.config.JsonTypeValidationConfig;
import skillup_workspace.dto.request.CourseRequestDTO;
import skillup_workspace.dto.request.ModalityRequestDTO;
import skillup_workspace.dto.response.CourseDetailResponseDTO;
import skillup_workspace.dto.response.CourseResponseDTO;
import skillup_workspace.entity.CourseStatus;
import skillup_workspace.exception.GlobalExceptionHandler;
import skillup_workspace.exception.ResourceNotFoundException;
import skillup_workspace.service.CourseService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
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

    private CourseController courseController;

    private ObjectMapper objectMapper;
    private Validator validator;

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

    private MockPart coursePart(String json) {
        return coursePart(json, MediaType.APPLICATION_JSON_VALUE);
    }

    private MockPart coursePart(String json, String contentType) {
        MockPart part = new MockPart(
                "course",
                "course.json",
                json.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
        part.getHeaders().setContentType(MediaType.parseMediaType(contentType));
        return part;
    }

    private org.springframework.test.web.servlet.ResultActions performCreate(String json) throws Exception {
        return mockMvc.perform(multipart("/api/v1/courses").part(coursePart(json)));
    }

    @BeforeEach
    void setUp() {
        Jackson2ObjectMapperBuilder objectMapperBuilder = new Jackson2ObjectMapperBuilder();
        new JsonTypeValidationConfig().strictJsonTypeCoercion().customize(objectMapperBuilder);
        objectMapper = objectMapperBuilder.build();
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        courseController = new CourseController(courseService, objectMapper, validator);
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

        given(courseService.createCourse(
                any(CourseRequestDTO.class),
                nullable(MultipartFile.class),
                nullable(MultipartFile.class)
        )).willReturn(createdResponse);

        mockMvc.perform(multipart("/api/v1/courses")
                        .part(coursePart(objectMapper.writeValueAsString(requestDTO))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /api/v1/courses - Debería aceptar la creación como borrador")
    void createCourse_ShouldAcceptDraftStatus() throws Exception {
        given(courseService.createCourse(
                any(CourseRequestDTO.class),
                nullable(MultipartFile.class),
                nullable(MultipartFile.class)
        )).willReturn(new CourseDetailResponseDTO());

        performCreate("""
                                {
                                  "title": "Borrador",
                                  "summary": "Resumen",
                                  "description": "Descripción",
                                  "category": "Diseño",
                                  "level": "Intermedio",
                                  "durationHours": 5,
                                  "durationWeeks": 1,
                                  "status": "BORRADOR",
                                  "modality": { "type": "EN VIVO" }
                                }
                                """)
                .andExpect(status().isCreated());

        Mockito.verify(courseService).createCourse(
                Mockito.argThat(request -> request.getStatus() == CourseStatus.BORRADOR),
                nullable(MultipartFile.class),
                nullable(MultipartFile.class)
        );
    }

    @Test
    @DisplayName("POST /api/v1/courses - Acepta JSON aunque la parte llegue como octet-stream")
    void createCourse_ShouldAcceptOctetStreamCoursePart() throws Exception {
        given(courseService.createCourse(
                any(CourseRequestDTO.class),
                nullable(MultipartFile.class),
                nullable(MultipartFile.class)
        )).willReturn(new CourseDetailResponseDTO());

        mockMvc.perform(multipart("/api/v1/courses")
                        .part(coursePart(
                                objectMapper.writeValueAsString(validCourseRequestDTO()),
                                MediaType.APPLICATION_OCTET_STREAM_VALUE
                        )))
                .andExpect(status().isCreated());

        Mockito.verify(courseService).createCourse(
                Mockito.argThat(request -> "Curso de Java".equals(request.getTitle())),
                nullable(MultipartFile.class),
                nullable(MultipartFile.class)
        );
    }

    @Test
    @DisplayName("POST /api/v1/courses - Valida campos requeridos tras deserializar la parte")
    void createCourse_ShouldValidateDeserializedCoursePart() throws Exception {
        performCreate("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message.title").value("El campo title es obligatorio"));

        Mockito.verify(courseService, Mockito.never()).createCourse(
                any(CourseRequestDTO.class),
                nullable(MultipartFile.class),
                nullable(MultipartFile.class)
        );
    }

    @Test
    @DisplayName("POST /api/v1/courses - Debería entregar los archivos opcionales al servicio")
    void createCourse_ShouldPassOptionalFilesToService() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "cover.webp", "image/webp", new byte[]{1, 2, 3}
        );
        MockMultipartFile syllabus = new MockMultipartFile(
                "syllabus", "program.pdf", "application/pdf", new byte[]{4, 5, 6}
        );
        given(courseService.createCourse(
                any(CourseRequestDTO.class),
                any(MultipartFile.class),
                any(MultipartFile.class)
        )).willReturn(new CourseDetailResponseDTO());

        mockMvc.perform(multipart("/api/v1/courses")
                        .part(coursePart(objectMapper.writeValueAsString(validCourseRequestDTO())))
                        .file(image)
                        .file(syllabus))
                .andExpect(status().isCreated());

        Mockito.verify(courseService).createCourse(
                any(CourseRequestDTO.class),
                Mockito.argThat(file -> file != null && "cover.webp".equals(file.getOriginalFilename())),
                Mockito.argThat(file -> file != null && "program.pdf".equals(file.getOriginalFilename()))
        );
    }

    @Test
    @DisplayName("POST /api/v1/courses - Debería retornar 400 para un valor numérico inválido")
    void createCourse_ShouldReturnBadRequestWhenNumericFieldHasInvalidType() throws Exception {
        performCreate("""
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
                                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        "El JSON contiene valores inválidos o incompatibles con el tipo esperado."
                ));

        Mockito.verify(courseService, Mockito.never()).createCourse(
                any(CourseRequestDTO.class),
                nullable(MultipartFile.class),
                nullable(MultipartFile.class)
        );
    }

    @Test
    @DisplayName("POST /api/v1/courses - Debería rechazar números para campos de texto")
    void createCourse_ShouldReturnBadRequestWhenTextFieldIsNumeric() throws Exception {
        performCreate("""
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
                                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        Mockito.verify(courseService, Mockito.never()).createCourse(
                any(CourseRequestDTO.class),
                nullable(MultipartFile.class),
                nullable(MultipartFile.class)
        );
    }

    @Test
    @DisplayName("POST /api/v1/courses - Debería rechazar texto para campos numéricos")
    void createCourse_ShouldReturnBadRequestWhenNumericFieldIsString() throws Exception {
        performCreate("""
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
                                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        Mockito.verify(courseService, Mockito.never()).createCourse(
                any(CourseRequestDTO.class),
                nullable(MultipartFile.class),
                nullable(MultipartFile.class)
        );
    }

    @Test
    @DisplayName("POST /api/v1/courses - Debería rechazar nextStartDate numérica")
    void createCourse_ShouldReturnBadRequestWhenNextStartDateIsNumeric() throws Exception {
        performCreate("""
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
                                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        Mockito.verify(courseService, Mockito.never()).createCourse(
                any(CourseRequestDTO.class),
                nullable(MultipartFile.class),
                nullable(MultipartFile.class)
        );
    }

    @Test
    @DisplayName("POST /api/v1/courses - Debería aceptar nextStartDate en formato ISO")
    void createCourse_ShouldAcceptNextStartDateInIsoFormat() throws Exception {
        given(courseService.createCourse(
                any(CourseRequestDTO.class),
                nullable(MultipartFile.class),
                nullable(MultipartFile.class)
        ))
                .willReturn(new CourseDetailResponseDTO());

        performCreate("""
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
                                """)
                .andExpect(status().isCreated());

        Mockito.verify(courseService).createCourse(
                Mockito.argThat(request -> java.time.LocalDate.of(2026, 11, 7).equals(request.getNextStartDate())),
                nullable(MultipartFile.class),
                nullable(MultipartFile.class)
        );
    }

    @Test
    @DisplayName("POST /api/v1/courses - Debería aceptar la duración exacta y el contenido grabado en minutos")
    void createCourse_ShouldAcceptScheduleWorkloadMinutes() throws Exception {
        given(courseService.createCourse(
                any(CourseRequestDTO.class),
                nullable(MultipartFile.class),
                nullable(MultipartFile.class)
        ))
                .willReturn(new CourseDetailResponseDTO());

        performCreate("""
                                {
                                  "title": "Curso híbrido",
                                  "summary": "Resumen",
                                  "description": "Descripción",
                                  "category": "Diseño",
                                  "level": "Intermedio",
                                  "durationHours": 13,
                                  "durationWeeks": 4,
                                  "schedule": {
                                    "days": "Lunes",
                                    "startTime": "17:30",
                                    "endTime": "19:00",
                                    "totalDurationMinutes": 750,
                                    "recordedContentMinutes": 390
                                  },
                                  "modality": {}
                                }
                                """)
                .andExpect(status().isCreated());

        Mockito.verify(courseService).createCourse(
                Mockito.argThat(request ->
                        request.getSchedule() != null
                                && Integer.valueOf(750).equals(request.getSchedule().getTotalDurationMinutes())
                                && Integer.valueOf(390).equals(request.getSchedule().getRecordedContentMinutes())
                ),
                nullable(MultipartFile.class),
                nullable(MultipartFile.class)
        );
    }

    @Test
    @DisplayName("POST /api/v1/courses - Debería rechazar duraciones negativas en el horario")
    void createCourse_ShouldRejectNegativeScheduleWorkloadMinutes() throws Exception {
        performCreate("""
                                {
                                  "title": "Curso grabado",
                                  "summary": "Resumen",
                                  "description": "Descripción",
                                  "category": "Diseño",
                                  "level": "Intermedio",
                                  "durationHours": 1,
                                  "durationWeeks": 1,
                                  "schedule": {
                                    "recordedContentMinutes": -1
                                  },
                                  "modality": {}
                                }
                                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        Mockito.verify(courseService, Mockito.never()).createCourse(
                any(CourseRequestDTO.class),
                nullable(MultipartFile.class),
                nullable(MultipartFile.class)
        );
    }

    @Test
    @DisplayName("POST /api/v1/courses - Debería permitir duración en semanas omitida para un curso grabado")
    void createCourse_ShouldAllowMissingDurationWeeksForRecordedCourse() throws Exception {
        given(courseService.createCourse(
                any(CourseRequestDTO.class),
                nullable(MultipartFile.class),
                nullable(MultipartFile.class)
        ))
                .willReturn(new CourseDetailResponseDTO());

        performCreate("""
                                {
                                  "title": "Curso grabado",
                                  "summary": "Resumen",
                                  "description": "Descripción",
                                  "category": "Diseño",
                                  "level": "Intermedio",
                                  "durationHours": 5,
                                  "modality": { "type": "GRABADO" },
                                  "schedule": { "recordedContentMinutes": 300, "totalDurationMinutes": 300 }
                                }
                                """)
                .andExpect(status().isCreated());

        Mockito.verify(courseService).createCourse(
                Mockito.argThat(request -> request.getDurationWeeks() == null),
                nullable(MultipartFile.class),
                nullable(MultipartFile.class)
        );
    }

    @Test
    @DisplayName("POST /api/v1/courses - Debería exigir semanas en cursos en vivo")
    void createCourse_ShouldRequireDurationWeeksForLiveCourse() throws Exception {
        performCreate("""
                                {
                                  "title": "Curso en vivo",
                                  "summary": "Resumen",
                                  "description": "Descripción",
                                  "category": "Diseño",
                                  "level": "Intermedio",
                                  "durationHours": 5,
                                  "modality": { "type": "EN VIVO" }
                                }
                                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        Mockito.verify(courseService, Mockito.never()).createCourse(
                any(CourseRequestDTO.class),
                nullable(MultipartFile.class),
                nullable(MultipartFile.class)
        );
    }

    @Test
    @DisplayName("PUT /api/v1/courses/{id} - Debería actualizar un curso y retornar HttpStatus 200 OK")
    void updateCourse_ShouldReturnUpdatedCourse() throws Exception {
        Long courseId = 1L;
        CourseRequestDTO requestDTO = validCourseRequestDTO();
        CourseDetailResponseDTO updatedResponse = new CourseDetailResponseDTO();

        given(courseService.updateCourse(
                eq(courseId),
                any(CourseRequestDTO.class),
                nullable(MultipartFile.class),
                nullable(MultipartFile.class)
        )).willReturn(updatedResponse);

        mockMvc.perform(multipart("/api/v1/courses/{id}", courseId)
                        .part(coursePart(objectMapper.writeValueAsString(requestDTO)))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/v1/courses/{id} - Acepta JSON aunque la parte llegue como octet-stream")
    void updateCourse_ShouldAcceptOctetStreamCoursePart() throws Exception {
        Long courseId = 1L;
        given(courseService.updateCourse(
                eq(courseId),
                any(CourseRequestDTO.class),
                nullable(MultipartFile.class),
                nullable(MultipartFile.class)
        )).willReturn(new CourseDetailResponseDTO());

        mockMvc.perform(multipart("/api/v1/courses/{id}", courseId)
                        .part(coursePart(
                                objectMapper.writeValueAsString(validCourseRequestDTO()),
                                MediaType.APPLICATION_OCTET_STREAM_VALUE
                        ))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /api/v1/courses/{id}/status - Debería actualizar el estado del curso")
    void updateCourseStatus_ShouldReturnUpdatedCourse() throws Exception {
        Long courseId = 1L;
        CourseDetailResponseDTO response = new CourseDetailResponseDTO();
        response.setStatus(CourseStatus.ARCHIVADO);
        given(courseService.updateCourseStatus(courseId, CourseStatus.ARCHIVADO)).willReturn(response);

        mockMvc.perform(patch("/api/v1/courses/{id}/status", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ARCHIVADO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVADO"));
    }

    @Test
    @DisplayName("PATCH /api/v1/courses/{id}/status - Debería rechazar estados inválidos")
    void updateCourseStatus_ShouldRejectInvalidStatus() throws Exception {
        mockMvc.perform(patch("/api/v1/courses/{id}/status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"UNKNOWN"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/v1/courses/{id}/status - Debería rechazar estado ausente")
    void updateCourseStatus_ShouldRejectMissingStatus() throws Exception {
        mockMvc.perform(patch("/api/v1/courses/{id}/status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
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
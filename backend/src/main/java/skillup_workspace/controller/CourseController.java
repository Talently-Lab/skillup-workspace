package skillup_workspace.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Part;
import jakarta.validation.Valid;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import skillup_workspace.dto.request.CourseMultipartRequestDTO;
import skillup_workspace.dto.request.CourseRequestDTO;
import skillup_workspace.dto.request.CourseStatusUpdateRequestDTO;
import skillup_workspace.dto.response.CourseDetailResponseDTO;
import skillup_workspace.dto.response.CourseResponseDTO;
import skillup_workspace.service.CourseService;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Tag(name = "Cursos", description = "Endpoints para la gestión y consulta de cursos")
public class CourseController {

    private final CourseService courseService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Operation(
            summary = "Obtener lista de cursos",
            description = "Retorna un listado de todos los cursos disponibles."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de cursos obtenida exitosamente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CourseResponseDTO.class)))
            )
    })
    @GetMapping
    public ResponseEntity<List<CourseResponseDTO>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    @Operation(
            summary = "Obtener detalle de un curso",
            description = "Retorna la información detallada de un curso según su ID."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Curso encontrado",
                    content = @Content(schema = @Schema(implementation = CourseDetailResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Curso no encontrado",
                    content = @Content
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<CourseDetailResponseDTO> getCourseById(
            @Parameter(description = "ID del curso a consultar", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    @Operation(
            summary = "Crear un nuevo curso",
            description = """
                    Crea un curso en POST /api/v1/courses usando multipart/form-data. La parte `course`
                    es obligatoria y contiene el JSON de CourseRequestDTO. Las partes opcionales `image`
                    y `syllabus` aceptan, respectivamente, una imagen compatible con ImageIO y un PDF.
                    El servidor convierte la imagen a WebP, guarda los archivos en R2 y administra sus enlaces.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Datos del curso y archivos opcionales",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = CourseMultipartRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "Crear curso de Diseño UX/UI",
                                    value = """
                                            {
                                              "course": {
                                                "title": "Diseño UX/UI Avanzado",
                                                "summary": "Aprendé los fundamentos del diseño de experiencias digitales y creá interfaces centradas en el usuario.",
                                                "description": "Este curso te va a permitir comprender el proceso de diseño desde la investigación hasta la creación de prototipos.",
                                                "category": "Diseño UX/UI",
                                                "level": "Avanzado",
                                                "durationHours": 40,
                                                "durationWeeks": 14,
                                                "nextStartDate": "2026-11-01",
                                                "schedule": {
                                                  "days": "Martes y Jueves",
                                                  "startTime": "18:00",
                                                  "endTime": "20:00"
                                                },
                                                "modality": {
                                                  "type": "EN VIVO",
                                                  "frequency": "2 clases semanales",
                                                  "durationPerClass": "2hs"
                                                },
                                                "learningGoals": [
                                                  "Comprender las necesidades del usuario.",
                                                  "Diseñar interfaces funcionales y atractivas.",
                                                  "Crear prototipos interactivos complejos."
                                                ],
                                                "objectives": [
                                                  "Dominar metodologías de investigación con usuarios.",
                                                  "Aprender a diseñar Wireframes y Prototipos en Figma."
                                                ],
                                                "contentModules": [
                                                  {
                                                    "moduleNumber": 1,
                                                    "title": "Introducción al Diseño UX",
                                                    "topics": [
                                                      "¿Qué es UX/UI?",
                                                      "Metodología Design Thinking"
                                                    ]
                                                  }
                                                ]
                                              }
                                            }
                                            """
                            ),
                            encoding = @Encoding(name = "course", contentType = MediaType.APPLICATION_JSON_VALUE)
                    )
            ),
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Curso creado exitosamente",
                    content = @Content(schema = @Schema(implementation = CourseDetailResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos de entrada inválidos",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Autenticación requerida o token inválido, mal formado o expirado",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Token válido, pero el usuario no tiene rol ADMIN",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "413",
                    description = "El archivo supera el límite configurado",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Almacenamiento R2 no disponible",
                    content = @Content
            )
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CourseDetailResponseDTO> createCourse(
            @Parameter(
                    description = "Datos del curso en JSON",
                    content = @Content(schema = @Schema(implementation = CourseRequestDTO.class))
            )
            @RequestPart("course") Part coursePart,
            @Parameter(description = "Imagen compatible con ImageIO (opcional); se almacena como WebP")
            @RequestPart(value = "image", required = false) MultipartFile image,
            @Parameter(description = "Programa PDF (opcional)")
            @RequestPart(value = "syllabus", required = false) MultipartFile syllabus) {
        CourseRequestDTO requestDTO = parseCoursePart(coursePart);
        CourseDetailResponseDTO createdCourse = courseService.createCourse(requestDTO, image, syllabus);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCourse);
    }

    @Operation(
            summary = "Actualizar un curso existente",
            description = """
                    Actualiza el curso indicado por {id} usando PUT /api/v1/courses/{id} y multipart/form-data.
                    La parte obligatoria `course` contiene el JSON completo de CourseRequestDTO. Las partes
                    opcionales `image` y `syllabus` aceptan, respectivamente, una imagen compatible con
                    ImageIO y un PDF. La imagen se convierte a WebP. Si se omite un archivo, se conserva el
                    recurso actual. Los enlaces de imagen y programa son administrados por el servidor.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Datos del curso y archivos opcionales. Si no se envía un archivo, se conserva el actual.",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = CourseMultipartRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "Actualizar curso de Diseño UX/UI",
                                    value = """
                                            {
                                              "course": {
                                                "title": "Diseño UX/UI Avanzado",
                                                "summary": "Aprendé los fundamentos del diseño de experiencias digitales y creá interfaces centradas en el usuario.",
                                                "description": "Este curso te va a permitir comprender el proceso de diseño desde la investigación hasta la creación de prototipos.",
                                                "category": "Diseño UX/UI",
                                                "level": "Avanzado",
                                                "durationHours": 40,
                                                "durationWeeks": 14,
                                                "nextStartDate": "2026-11-01",
                                                "schedule": {
                                                  "days": "Martes y Jueves",
                                                  "startTime": "18:00",
                                                  "endTime": "20:00"
                                                },
                                                "modality": {
                                                  "type": "EN VIVO",
                                                  "frequency": "2 clases semanales",
                                                  "durationPerClass": "2hs"
                                                },
                                                "learningGoals": [
                                                  "Comprender las necesidades del usuario.",
                                                  "Diseñar interfaces funcionales y atractivas.",
                                                  "Crear prototipos interactivos complejos."
                                                ],
                                                "objectives": [
                                                  "Dominar metodologías de investigación con usuarios.",
                                                  "Aprender a diseñar Wireframes y Prototipos en Figma."
                                                ],
                                                "contentModules": [
                                                  {
                                                    "moduleNumber": 1,
                                                    "title": "Introducción al Diseño UX",
                                                    "topics": [
                                                      "¿Qué es UX/UI?",
                                                      "Metodología Design Thinking"
                                                    ]
                                                  }
                                                ]
                                              }
                                            }
                                            """
                            ),
                            encoding = @Encoding(name = "course", contentType = MediaType.APPLICATION_JSON_VALUE)
                    )
            ),
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Curso actualizado exitosamente",
                    content = @Content(schema = @Schema(implementation = CourseDetailResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos de entrada inválidos",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Curso no encontrado",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Autenticación requerida o token inválido, mal formado o expirado",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Token válido, pero el usuario no tiene rol ADMIN",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "413",
                    description = "El archivo supera el límite configurado",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Almacenamiento R2 no disponible",
                    content = @Content
            )
    })
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CourseDetailResponseDTO> updateCourse(
            @Parameter(description = "ID del curso a actualizar", example = "1")
            @PathVariable Long id,
            @Parameter(
                    description = "Datos del curso en JSON",
                    content = @Content(schema = @Schema(implementation = CourseRequestDTO.class))
            )
            @RequestPart("course") Part coursePart,
            @Parameter(description = "Imagen compatible con ImageIO (opcional); se almacena como WebP")
            @RequestPart(value = "image", required = false) MultipartFile image,
            @Parameter(description = "Programa PDF (opcional)")
            @RequestPart(value = "syllabus", required = false) MultipartFile syllabus) {
        CourseRequestDTO requestDTO = parseCoursePart(coursePart);
        return ResponseEntity.ok(courseService.updateCourse(id, requestDTO, image, syllabus));
    }

    @Operation(
            summary = "Actualizar estado de un curso",
            description = "Actualiza el estado persistente del curso sin modificar sus demás datos.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Estado actualizado exitosamente",
                    content = @Content(schema = @Schema(implementation = CourseDetailResponseDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Estado inválido o ausente", content = @Content),
            @ApiResponse(responseCode = "404", description = "Curso no encontrado", content = @Content),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida", content = @Content),
            @ApiResponse(responseCode = "403", description = "Se requiere rol ADMIN", content = @Content)
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<CourseDetailResponseDTO> updateCourseStatus(
            @Parameter(description = "ID del curso", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody CourseStatusUpdateRequestDTO request) {
        return ResponseEntity.ok(courseService.updateCourseStatus(id, request.getStatus()));
    }

    private CourseRequestDTO parseCoursePart(Part coursePart) {
        CourseRequestDTO requestDTO;
        try {
            requestDTO = objectMapper.readValue(coursePart.getInputStream(), CourseRequestDTO.class);
        } catch (JsonProcessingException ex) {
            throw new HttpMessageNotReadableException("La parte 'course' debe contener JSON válido.", ex);
        } catch (IOException ex) {
            throw new HttpMessageNotReadableException("No se pudo leer la parte 'course'.", ex);
        }

        if (requestDTO == null) {
            throw new HttpMessageNotReadableException(
                    "La parte 'course' no puede ser null.",
                    new IllegalArgumentException("El JSON de curso no puede ser null.")
            );
        }

        Set<ConstraintViolation<CourseRequestDTO>> violations = validator.validate(requestDTO);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return requestDTO;
    }

    @Operation(
            summary = "Eliminar un curso",
            description = "Elimina un curso del sistema mediante su ID.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Curso eliminado exitosamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Curso no encontrado",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "No se puede eliminar el curso porque tiene inscripciones asociadas",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Autenticación requerida o token inválido, mal formado o expirado",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Token válido, pero el usuario no tiene rol ADMIN",
                    content = @Content
            )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(
            @Parameter(description = "ID del curso a eliminar", example = "1")
            @PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }
}
package skillup_workspace.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import skillup_workspace.dto.request.CourseRequestDTO;
import skillup_workspace.dto.response.CourseDetailResponseDTO;
import skillup_workspace.dto.response.CourseResponseDTO;
import skillup_workspace.service.CourseService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Tag(name = "Cursos", description = "Endpoints para la gestión y consulta de cursos")
public class CourseController {

    private final CourseService courseService;

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
            description = "Registra un nuevo curso en el sistema.",
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
                    description = "No autorizado",
                    content = @Content
            )
    })
    @PostMapping
    public ResponseEntity<CourseDetailResponseDTO> createCourse(@Valid @RequestBody CourseRequestDTO requestDTO) {
        CourseDetailResponseDTO createdCourse = courseService.createCourse(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCourse);
    }

    @Operation(
            summary = "Actualizar un curso existente",
            description = "Actualiza los datos de un curso según su ID.",
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
            )
    })
    @PutMapping("/{id}")
    public ResponseEntity<CourseDetailResponseDTO> updateCourse(
            @Parameter(description = "ID del curso a actualizar", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody CourseRequestDTO requestDTO) {
        return ResponseEntity.ok(courseService.updateCourse(id, requestDTO));
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
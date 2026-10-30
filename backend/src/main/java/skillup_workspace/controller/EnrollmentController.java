package skillup_workspace.controller;

import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import skillup_workspace.dto.request.EnrollmentRequestDTO;
import skillup_workspace.dto.response.EnrollmentResponseDTO;
import skillup_workspace.service.EnrollmentService;

@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
@Tag(name = "Inscripciones", description = "Endpoints para la gestión de inscripciones a cursos")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @Operation(
            summary = "Inscribirse a un curso",
            description = "Permite a un alumno autenticado inscribirse a un curso activo del catálogo.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Inscripción exitosa",
                    content = @Content(schema = @Schema(implementation = EnrollmentResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (ej. ID de curso vacío)", content = @Content),
            @ApiResponse(responseCode = "401", description = "No autorizado (Token faltante o inválido)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Curso o usuario no encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Conflicto (El curso está inactivo o el usuario ya está inscripto)", content = @Content)
    })
    @PostMapping
    public ResponseEntity<EnrollmentResponseDTO> enroll(@Valid @RequestBody EnrollmentRequestDTO request) {
        EnrollmentResponseDTO response = enrollmentService.enrollUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
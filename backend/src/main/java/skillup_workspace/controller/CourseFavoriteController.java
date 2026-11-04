package skillup_workspace.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import skillup_workspace.dto.response.CourseFavoriteResponseDTO;
import skillup_workspace.service.CourseFavoriteService;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/favorite")
@RequiredArgsConstructor
@Tag(name = "Favoritos de cursos", description = "Gestión de cursos favoritos de alumnos")
public class CourseFavoriteController {

    private final CourseFavoriteService favoriteService;

    @GetMapping
    @Operation(
            summary = "Consultar si un curso es favorito",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado del favorito"),
            @ApiResponse(responseCode = "401", description = "Token faltante o inválido"),
            @ApiResponse(responseCode = "403", description = "Solo disponible para alumnos"),
            @ApiResponse(responseCode = "404", description = "Curso no encontrado")
    })
    public CourseFavoriteResponseDTO getFavoriteStatus(@PathVariable Long courseId) {
        return favoriteService.getFavoriteStatus(courseId);
    }

    @PutMapping
    @Operation(
            summary = "Agregar un curso a favoritos",
            description = "Operación idempotente: repetirla conserva un único favorito.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Curso agregado a favoritos"),
            @ApiResponse(responseCode = "401", description = "Token faltante o inválido"),
            @ApiResponse(responseCode = "403", description = "Solo disponible para alumnos"),
            @ApiResponse(responseCode = "404", description = "Curso no encontrado")
    })
    public ResponseEntity<Void> addFavorite(@PathVariable Long courseId) {
        favoriteService.addFavorite(courseId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(
            summary = "Quitar un curso de favoritos",
            description = "Operación idempotente: repetirla mantiene el curso fuera de favoritos.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Curso quitado de favoritos"),
            @ApiResponse(responseCode = "401", description = "Token faltante o inválido"),
            @ApiResponse(responseCode = "403", description = "Solo disponible para alumnos"),
            @ApiResponse(responseCode = "404", description = "Curso no encontrado")
    })
    public ResponseEntity<Void> removeFavorite(@PathVariable Long courseId) {
        favoriteService.removeFavorite(courseId);
        return ResponseEntity.noContent().build();
    }
}

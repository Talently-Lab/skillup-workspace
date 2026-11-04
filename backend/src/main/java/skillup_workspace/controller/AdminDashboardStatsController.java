package skillup_workspace.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import skillup_workspace.dto.response.AdminDashboardStatsResponseDTO;
import skillup_workspace.service.AdminDashboardStatsService;

@RestController
@RequestMapping("/api/v1/admin/dashboard-stats")
@RequiredArgsConstructor
@Tag(name = "Estadísticas administrativas", description = "Indicadores del dashboard administrativo")
public class AdminDashboardStatsController {

    private final AdminDashboardStatsService dashboardStatsService;

    @GetMapping
    @Operation(
            summary = "Obtener indicadores del dashboard",
            description = "Retorna conteos de cursos e inscripciones de alumnos. " +
                    "El conteo de cursos finalizados no está disponible hasta que exista estado de finalización.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Indicadores obtenidos"),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida"),
            @ApiResponse(responseCode = "403", description = "Se requiere rol ADMIN")
    })
    public ResponseEntity<AdminDashboardStatsResponseDTO> getDashboardStats() {
        return ResponseEntity.ok(dashboardStatsService.getDashboardStats());
    }
}

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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import skillup_workspace.dto.request.CompleteUploadRequest;
import skillup_workspace.dto.request.PresignUploadRequest;
import skillup_workspace.dto.response.CompletedUploadResponse;
import skillup_workspace.dto.response.PresignedUploadResponse;
import skillup_workspace.storage.R2StorageService;

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
@Tag(name = "Archivos", description = "Carga validada de imágenes y programas públicos de cursos en R2")
public class UploadController {

    private final R2StorageService r2StorageService;

    @PostMapping("/presign")
    @Operation(
            summary = "Autorizar carga directa a R2",
            description = "Genera una clave y una URL PUT de corta duración para un archivo de curso.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Autorización generada",
                    content = @Content(schema = @Schema(implementation = PresignedUploadResponse.class))),
            @ApiResponse(responseCode = "400", description = "Tipo, extensión o tamaño no permitido", content = @Content),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida", content = @Content),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN puede cargar archivos", content = @Content),
            @ApiResponse(responseCode = "503", description = "Almacenamiento R2 no configurado", content = @Content)
    })
    public ResponseEntity<PresignedUploadResponse> presign(
            @Valid @RequestBody PresignUploadRequest request) {
        return ResponseEntity.ok(r2StorageService.createPresignedUpload(request));
    }

    @PostMapping("/complete")
    @Operation(
            summary = "Verificar archivo cargado",
            description = "Valida tamaño, MIME y firma del objeto antes de devolver su URL pública de curso.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Archivo verificado",
                    content = @Content(schema = @Schema(implementation = CompletedUploadResponse.class))),
            @ApiResponse(responseCode = "400", description = "Archivo ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida", content = @Content),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN puede verificar cargas", content = @Content),
            @ApiResponse(responseCode = "503", description = "Almacenamiento R2 no configurado", content = @Content)
    })
    public ResponseEntity<CompletedUploadResponse> complete(
            @Valid @RequestBody CompleteUploadRequest request) {
        return ResponseEntity.ok(r2StorageService.completeUpload(request));
    }
}

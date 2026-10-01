package skillup_workspace.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import skillup_workspace.dto.request.LoginRequest;
import skillup_workspace.dto.request.RegisterRequest;
import skillup_workspace.dto.response.LoginResponse;
import skillup_workspace.dto.response.RegisterResponse;
import skillup_workspace.entity.User;
import skillup_workspace.security.JwtService;
import skillup_workspace.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Endpoints para registro e inicio de sesión de usuarios")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @Operation(
            summary = "Registrar un nuevo usuario",
            description = "Crea una nueva cuenta de usuario con la información proporcionada."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Usuario registrado exitosamente",
                    content = @Content(schema = @Schema(implementation = RegisterResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos de entrada inválidos o email ya registrado",
                    content = @Content
            )
    })
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        User user = authService.register(request);

        RegisterResponse response = new RegisterResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Iniciar sesión",
            description = "Autentica al usuario mediante email y contraseña, devoliendo un token JWT."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Autenticación exitosa",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Credenciales inválidas",
                    content = @Content
            )
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        User user = authService.login(request);
        String jwToken = jwtService.generateToken(user);

        LoginResponse response = new LoginResponse(
                jwToken,
                "Bearer",
                user.getId(),
                user.getEmail(),
                user.getRole()
        );

        return ResponseEntity.ok(response);
    }
}
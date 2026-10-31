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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import skillup_workspace.dto.request.LoginRequest;
import skillup_workspace.dto.request.RegisterRequest;
import skillup_workspace.entity.User;
import skillup_workspace.exception.GlobalExceptionHandler;
import skillup_workspace.security.JwtService;
import skillup_workspace.service.AuthService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;
    private User mockUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setFirstName("Juan");
        mockUser.setLastName("Pérez");
        mockUser.setEmail("juan.perez@example.com");
        mockUser.setRole("USER");
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Debería registrar un usuario y retornar HttpStatus 201 CREATED")
    void register_ShouldReturnCreatedResponse() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setFirstName("Juan");
        registerRequest.setLastName("Pérez");
        registerRequest.setEmail("juan.perez@example.com");
        registerRequest.setPassword("password123");

        given(authService.register(any(RegisterRequest.class))).willReturn(mockUser);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Juan"))
                .andExpect(jsonPath("$.lastName").value("Pérez"))
                .andExpect(jsonPath("$.email").value("juan.perez@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Debería retornar 400 si falta un campo obligatorio")
    void register_ShouldReturnBadRequestWhenRequiredFieldsAreMissing() throws Exception {
        String[][] requests = {
                {"{\"lastName\":\"Pérez\",\"email\":\"juan.perez@example.com\",\"password\":\"password123\"}", "firstName"},
                {"{\"firstName\":\"Juan\",\"email\":\"juan.perez@example.com\",\"password\":\"password123\"}", "lastName"},
                {"{\"firstName\":\"Juan\",\"lastName\":\"Pérez\",\"password\":\"password123\"}", "email"},
                {"{\"firstName\":\"Juan\",\"lastName\":\"Pérez\",\"email\":\"juan.perez@example.com\"}", "password"}
        };

        for (String[] request : requests) {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request[0]))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message." + request[1]).exists());
        }

        Mockito.verify(authService, Mockito.never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Debería retornar 400 con campos faltantes o vacíos")
    void login_ShouldReturnBadRequestWhenRequiredFieldsAreMissingOrBlank() throws Exception {
        String[] requests = {
                "{\"password\":\"password123\"}",
                "{\"email\":\"juan.perez@example.com\"}",
                "{\"email\":\" \",\"password\":\"password123\"}",
                "{\"email\":\"juan.perez@example.com\",\"password\":\" \"}"
        };

        for (String request : requests) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        Mockito.verify(authService, Mockito.never()).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Debería retornar 401 con credenciales incorrectas")
    void login_ShouldReturnUnauthorizedWhenCredentialsAreIncorrect() throws Exception {
        given(authService.login(any(LoginRequest.class)))
                .willThrow(new SecurityException("Credenciales inválidas"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"juan.perez@example.com\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - No debería exponer mensajes internos")
    void register_ShouldHideInternalExceptionMessages() throws Exception {
        given(authService.register(any(RegisterRequest.class)))
                .willThrow(new IllegalArgumentException("rawPassword cannot be null"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Juan\",\"lastName\":\"Pérez\","
                                + "\"email\":\"juan.perez@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La solicitud contiene datos inválidos."))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("rawPassword")
                )));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Debería autenticar al usuario y retornar un Token JWT con HttpStatus 200 OK")
    void login_ShouldReturnOkResponseWithToken() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("juan.perez@example.com");
        loginRequest.setPassword("password123");

        String fakeToken = "eyJhbGciOiJIUzI1NiJ9.fakeTokenString";

        given(authService.login(any(LoginRequest.class))).willReturn(mockUser);
        given(jwtService.generateToken(mockUser)).willReturn(fakeToken);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(fakeToken))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("juan.perez@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }
}
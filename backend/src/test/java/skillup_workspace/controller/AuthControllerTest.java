package skillup_workspace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import skillup_workspace.dto.request.LoginRequest;
import skillup_workspace.dto.request.RegisterRequest;
import skillup_workspace.entity.User;
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
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
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
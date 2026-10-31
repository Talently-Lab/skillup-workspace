package skillup_workspace.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import skillup_workspace.dto.request.LoginRequest;
import skillup_workspace.entity.User;
import skillup_workspace.exception.GlobalExceptionHandler;
import skillup_workspace.security.JwtService;
import skillup_workspace.service.AuthService;

import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthTokenRoleTest {

    private static final String SECRET = Base64.getEncoder()
            .encodeToString("01234567890123456789012345678901".getBytes());

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 60_000L);

        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, jwtService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void loginGeneratesJwtWithRoleForAdminAndStudent() throws Exception {
        User admin = user("admin@example.com", "ADMIN");
        User student = user("student@example.com", "ALUMNO");
        given(authService.login(any(LoginRequest.class))).willAnswer(invocation -> {
            LoginRequest request = invocation.getArgument(0);
            return Map.of(admin.getEmail(), admin, student.getEmail(), student).get(request.getEmail());
        });

        assertLoginTokenContainsRole(admin);
        assertLoginTokenContainsRole(student);
    }

    private void assertLoginTokenContainsRole(User user) throws Exception {
        String request = """
                {"email":"%s","password":"password123"}
                """.formatted(user.getEmail());

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Decoders.BASE64.decode(SECRET))
                .build()
                .parseClaimsJws(response.get("token").asText())
                .getBody();

        assertEquals(user.getEmail(), claims.getSubject());
        assertEquals(user.getRole(), claims.get("role", String.class));
    }

    private User user(String email, String role) {
        User user = new User();
        user.setId(1L);
        user.setEmail(email);
        user.setRole(role);
        return user;
    }
}

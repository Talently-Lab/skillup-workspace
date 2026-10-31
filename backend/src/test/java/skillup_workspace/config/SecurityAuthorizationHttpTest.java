package skillup_workspace.config;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import skillup_workspace.controller.CourseController;
import skillup_workspace.entity.User;
import skillup_workspace.exception.GlobalExceptionHandler;
import skillup_workspace.exception.ResourceNotFoundException;
import skillup_workspace.repository.UserRepository;
import skillup_workspace.security.JwtAuthenticationFilter;
import skillup_workspace.security.JwtService;
import skillup_workspace.service.CourseService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest(
        classes = SecurityAuthorizationHttpTest.HttpTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class SecurityAuthorizationHttpTest {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private CourseService courseService;

    @Test
    void deleteWithoutTokenReturns401JsonOverHttp() throws Exception {
        HttpResponse<String> response = deleteCourse(null);

        assertEquals(401, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
        assertTrue(response.body().contains("\"status\":401"));
        assertTrue(response.body().contains("No autorizado"));
    }

    @Test
    void deleteWithEmptyBearerTokenReturns401JsonOverHttp() throws Exception {
        HttpResponse<String> response = deleteCourse("");

        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("\"status\":401"));
    }

    @Test
    void deleteWithInvalidTokenReturns401JsonOverHttp() throws Exception {
        given(jwtService.extractUsername("invalid"))
                .willThrow(new MalformedJwtException("invalid"));

        HttpResponse<String> response = deleteCourse("invalid");

        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("\"status\":401"));
    }

    @Test
    void deleteWithInvalidSignatureReturns401JsonOverHttp() throws Exception {
        given(jwtService.extractUsername("bad-signature"))
                .willThrow(new SignatureException("invalid signature"));

        HttpResponse<String> response = deleteCourse("bad-signature");

        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("\"status\":401"));
    }

    @Test
    void deleteWithExpiredTokenReturns401JsonOverHttp() throws Exception {
        given(jwtService.extractUsername("expired"))
                .willThrow(new ExpiredJwtException(null, null, "expired"));

        HttpResponse<String> response = deleteCourse("expired");

        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("\"status\":401"));
    }

    @Test
    void deleteWithStudentTokenReturns403WithoutErrorDispatchOverHttp() throws Exception {
        User student = user("student@example.com", "ALUMNO");
        given(jwtService.extractUsername("student-token")).willReturn(student.getEmail());
        given(userRepository.findByEmail(student.getEmail())).willReturn(Optional.of(student));
        given(jwtService.isTokenValid("student-token", student)).willReturn(true);

        HttpResponse<String> response = deleteCourse("student-token");

        assertEquals(403, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
        assertTrue(response.body().contains("\"status\":403"));
        assertTrue(response.body().contains("No tiene permisos"));
        verifyNoInteractions(courseService);
    }

    @Test
    void deleteWithAdminTokenAndExistingCourseReturns204OverHttp() throws Exception {
        User admin = user("admin@example.com", "ADMIN");
        given(jwtService.extractUsername("admin-token")).willReturn(admin.getEmail());
        given(userRepository.findByEmail(admin.getEmail())).willReturn(Optional.of(admin));
        given(jwtService.isTokenValid("admin-token", admin)).willReturn(true);
        willDoNothing().given(courseService).deleteCourse(2L);

        HttpResponse<String> response = deleteCourse("admin-token");

        assertEquals(204, response.statusCode());
        verify(courseService).deleteCourse(2L);
    }

    @Test
    void deleteWithAdminTokenAndMissingCourseReturns404OverHttp() throws Exception {
        User admin = user("admin@example.com", "ADMIN");
        given(jwtService.extractUsername("admin-token")).willReturn(admin.getEmail());
        given(userRepository.findByEmail(admin.getEmail())).willReturn(Optional.of(admin));
        given(jwtService.isTokenValid("admin-token", admin)).willReturn(true);
        willThrow(new ResourceNotFoundException("Curso no encontrado con el id: 2"))
                .given(courseService).deleteCourse(2L);

        HttpResponse<String> response = deleteCourse("admin-token");

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("\"status\":404"));
    }

    private HttpResponse<String> deleteCourse(String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/courses/2"))
                .DELETE();
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return HTTP_CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private User user(String email, String role) {
        User user = new User();
        user.setId(2L);
        user.setEmail(email);
        user.setRole(role);
        return user;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class
    })
    @Import({
            SecurityConfig.class,
            JwtAuthenticationFilter.class,
            CourseController.class,
            GlobalExceptionHandler.class
    })
    static class HttpTestApplication {
    }
}

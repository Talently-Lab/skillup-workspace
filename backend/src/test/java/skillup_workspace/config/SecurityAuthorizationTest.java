package skillup_workspace.config;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import skillup_workspace.controller.CourseController;
import skillup_workspace.entity.User;
import skillup_workspace.repository.UserRepository;
import skillup_workspace.security.JwtAuthenticationFilter;
import skillup_workspace.security.JwtService;
import skillup_workspace.service.CourseService;

import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private CourseService courseService;

    @Test
    void deleteCourseWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteCourseWithEmptyBearerTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/1")
                        .header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteCourseWithExpiredTokenReturnsUnauthorized() throws Exception {
        given(jwtService.extractUsername("expired"))
                .willThrow(new ExpiredJwtException(null, null, "expired"));

        mockMvc.perform(delete("/api/v1/courses/1")
                        .header("Authorization", "Bearer expired"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteCourseWithInvalidTokenReturnsUnauthorized() throws Exception {
        given(jwtService.extractUsername("invalid"))
                .willThrow(new MalformedJwtException("invalid"));

        mockMvc.perform(delete("/api/v1/courses/1")
                        .header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteCourseWithStudentTokenReturnsForbidden() throws Exception {
        User student = new User();
        student.setEmail("student@example.com");
        student.setRole("ALUMNO");

        given(jwtService.extractUsername("student-token")).willReturn(student.getEmail());
        given(userRepository.findByEmail(student.getEmail())).willReturn(Optional.of(student));
        given(jwtService.isTokenValid("student-token", student)).willReturn(true);

        mockMvc.perform(delete("/api/v1/courses/1")
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteCourseWithRolePrefixedStudentTokenReturnsForbidden() throws Exception {
        User student = new User();
        student.setEmail("student@example.com");
        student.setRole("ROLE_ALUMNO");

        given(jwtService.extractUsername("student-token")).willReturn(student.getEmail());
        given(userRepository.findByEmail(student.getEmail())).willReturn(Optional.of(student));
        given(jwtService.isTokenValid("student-token", student)).willReturn(true);

        mockMvc.perform(delete("/api/v1/courses/1")
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteCourseWithAdminTokenIsAuthorized() throws Exception {
        User admin = new User();
        admin.setEmail("admin@example.com");
        admin.setRole("ADMIN");

        given(jwtService.extractUsername("admin-token")).willReturn(admin.getEmail());
        given(userRepository.findByEmail(admin.getEmail())).willReturn(Optional.of(admin));
        given(jwtService.isTokenValid("admin-token", admin)).willReturn(true);

        mockMvc.perform(delete("/api/v1/courses/1")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isNoContent());
    }
}

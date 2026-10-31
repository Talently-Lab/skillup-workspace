package skillup_workspace.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import skillup_workspace.entity.Course;
import skillup_workspace.entity.Enrollment;
import skillup_workspace.entity.User;
import skillup_workspace.repository.CourseRepository;
import skillup_workspace.repository.EnrollmentRepository;
import skillup_workspace.repository.UserRepository;
import skillup_workspace.security.JwtService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:course-hard-delete;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.show-sql=false",
                "jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
                "jwt.expiration=86400000"
        }
)
class CourseHardDeleteIntegrationTest {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final String COURSE_REQUEST = """
            {
              "title": "Curso de prueba",
              "summary": "Resumen",
              "description": "Descripción",
              "category": "Programación",
              "level": "Inicial",
              "durationHours": 12,
              "durationWeeks": 3,
              "modality": {}
            }
            """;

    @LocalServerPort
    private int port;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User admin;
    private User student;

    @BeforeEach
    void setUp() {
        enrollmentRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();

        admin = saveUser("admin-delete@example.com", "ADMIN");
        student = saveUser("student-delete@example.com", "ALUMNO");
    }

    @Test
    void firstDeletePhysicallyRemovesCourseAndReturns204WithoutBody() throws Exception {
        Course course = saveCourse();

        HttpResponse<String> response = delete(course.getId(), jwtService.generateToken(admin));

        assertEquals(204, response.statusCode());
        assertTrue(response.body().isEmpty());
        assertFalse(courseRepository.existsById(course.getId()));
        assertEquals(0, jdbcTemplate.queryForObject(
                "select count(*) from courses where course_id = ?",
                Integer.class,
                course.getId()
        ));
    }

    @Test
    void secondDeleteOfSameCourseReturns404() throws Exception {
        Course course = saveCourse();
        String adminToken = jwtService.generateToken(admin);

        assertEquals(204, delete(course.getId(), adminToken).statusCode());
        HttpResponse<String> secondResponse = delete(course.getId(), adminToken);

        assertEquals(404, secondResponse.statusCode());
        assertTrue(secondResponse.body().contains("\"status\":404"));
        assertFalse(courseRepository.existsById(course.getId()));
    }

    @Test
    void deleteOfNeverExistingCourseReturns404() throws Exception {
        HttpResponse<String> response = delete(Long.MAX_VALUE, jwtService.generateToken(admin));

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("\"status\":404"));
    }

    @Test
    void studentCannotDeleteCourseAndCourseRemains() throws Exception {
        Course course = saveCourse();

        HttpResponse<String> response = delete(course.getId(), jwtService.generateToken(student));

        assertEquals(403, response.statusCode());
        assertTrue(courseRepository.existsById(course.getId()));
    }

    @Test
    void deleteWithoutTokenReturns401AndCourseRemains() throws Exception {
        Course course = saveCourse();

        HttpResponse<String> response = delete(course.getId(), null);

        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("\"status\":401"));
        assertTrue(courseRepository.existsById(course.getId()));
    }

    @Test
    void courseWithEnrollmentsIsNotDeletedOrphaningTheEnrollment() throws Exception {
        Course course = saveCourse();
        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);
        enrollment.setUser(student);
        enrollment.setStatus("ACTIVE");
        enrollmentRepository.saveAndFlush(enrollment);

        HttpResponse<String> response = delete(course.getId(), jwtService.generateToken(admin));

        assertEquals(409, response.statusCode());
        assertTrue(courseRepository.existsById(course.getId()));
        assertTrue(enrollmentRepository.existsById(enrollment.getId()));
    }

    @Test
    void studentCannotCreateCourseAndGets403Json() throws Exception {
        HttpResponse<String> response = send(
                HttpMethod.POST,
                "/api/v1/courses",
                jwtService.generateToken(student),
                COURSE_REQUEST
        );

        assertEquals(403, response.statusCode());
        assertTrue(response.body().isEmpty());
        assertEquals(0, courseRepository.count());
    }

    @Test
    void studentCannotCreateCourseEvenWhenRequestBodyIsInvalid() throws Exception {
        HttpResponse<String> response = send(
                HttpMethod.POST,
                "/api/v1/courses",
                jwtService.generateToken(student),
                "{"
        );

        assertEquals(403, response.statusCode());
        assertTrue(response.body().isEmpty());
        assertEquals(0, courseRepository.count());
    }

    @Test
    void studentCannotEditCourseAndGets403Json() throws Exception {
        Course course = saveCourse();
        HttpResponse<String> response = send(
                HttpMethod.PUT,
                "/api/v1/courses/" + course.getId(),
                jwtService.generateToken(student),
                COURSE_REQUEST.replace("Curso de prueba", "Intento de cambio")
        );

        assertEquals(403, response.statusCode());
        assertTrue(response.body().contains("\"status\":403"));
        assertEquals("Curso de prueba", courseRepository.findById(course.getId()).orElseThrow().getTitle());
    }

    @Test
    void createCourseWithoutTokenReturns401Json() throws Exception {
        HttpResponse<String> response = send(
                HttpMethod.POST,
                "/api/v1/courses",
                null,
                COURSE_REQUEST
        );

        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("\"status\":401"));
        assertEquals(0, courseRepository.count());
    }

    @Test
    void editCourseWithoutTokenReturns401Json() throws Exception {
        Course course = saveCourse();
        HttpResponse<String> response = send(
                HttpMethod.PUT,
                "/api/v1/courses/" + course.getId(),
                null,
                COURSE_REQUEST
        );

        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("\"status\":401"));
        assertEquals("Curso de prueba", courseRepository.findById(course.getId()).orElseThrow().getTitle());
    }

    @Test
    void createCourseWithInvalidTokenReturns401Json() throws Exception {
        HttpResponse<String> response = send(
                HttpMethod.POST,
                "/api/v1/courses",
                "invalid-token",
                COURSE_REQUEST
        );

        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("\"status\":401"));
    }

    @Test
    void createCourseWithEmptyTokenReturns401Json() throws Exception {
        HttpResponse<String> response = send(
                HttpMethod.POST,
                "/api/v1/courses",
                "",
                COURSE_REQUEST
        );

        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("\"status\":401"));
        assertEquals(0, courseRepository.count());
    }

    @Test
    void editCourseWithInvalidTokenReturns401Json() throws Exception {
        Course course = saveCourse();
        HttpResponse<String> response = send(
                HttpMethod.PUT,
                "/api/v1/courses/" + course.getId(),
                "invalid-token",
                COURSE_REQUEST
        );

        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("\"status\":401"));
    }

    @Test
    void editCourseWithEmptyTokenReturns401Json() throws Exception {
        Course course = saveCourse();
        HttpResponse<String> response = send(
                HttpMethod.PUT,
                "/api/v1/courses/" + course.getId(),
                "",
                COURSE_REQUEST
        );

        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("\"status\":401"));
        assertEquals("Curso de prueba", courseRepository.findById(course.getId()).orElseThrow().getTitle());
    }

    @Test
    void adminCanCreateCourseAndReceives201() throws Exception {
        HttpResponse<String> response = send(
                HttpMethod.POST,
                "/api/v1/courses",
                jwtService.generateToken(admin),
                COURSE_REQUEST
        );

        assertEquals(201, response.statusCode());
        assertEquals(1, courseRepository.count());
        assertTrue(response.body().contains("\"title\":\"Curso de prueba\""));
    }

    @Test
    void adminCanEditCourseAndReceives200() throws Exception {
        Course course = saveCourse();
        HttpResponse<String> response = send(
                HttpMethod.PUT,
                "/api/v1/courses/" + course.getId(),
                jwtService.generateToken(admin),
                COURSE_REQUEST.replace("Curso de prueba", "Curso actualizado")
        );

        assertEquals(200, response.statusCode());
        assertEquals("Curso actualizado", courseRepository.findById(course.getId()).orElseThrow().getTitle());
    }

    private Course saveCourse() {
        Course course = new Course();
        course.setTitle("Curso de prueba");
        course.setSummary("Resumen");
        course.setDescription("Descripción");
        course.setCategory("Programación");
        course.setLevel("Inicial");
        course.setDurationHours(12);
        course.setDurationWeeks(3);
        return courseRepository.saveAndFlush(course);
    }

    private User saveUser(String email, String role) {
        User user = new User();
        user.setFirstName("Usuario");
        user.setLastName("Prueba");
        user.setEmail(email);
        user.setPasswordHash("hash");
        user.setRole(role);
        return userRepository.saveAndFlush(user);
    }

    private HttpResponse<String> delete(Long courseId, String token) throws Exception {
        return send(HttpMethod.DELETE, "/api/v1/courses/" + courseId, token, null);
    }

    private HttpResponse<String> send(HttpMethod method, String path, String token, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path));
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (body != null) {
            request.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .method(method.name(), HttpRequest.BodyPublishers.ofString(body));
        } else {
            request.method(method.name(), HttpRequest.BodyPublishers.noBody());
        }
        return HTTP_CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}

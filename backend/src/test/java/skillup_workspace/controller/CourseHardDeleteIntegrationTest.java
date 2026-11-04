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
import skillup_workspace.entity.CourseStatus;
import skillup_workspace.entity.Enrollment;
import skillup_workspace.entity.User;
import skillup_workspace.repository.CourseRepository;
import skillup_workspace.repository.CourseFavoriteRepository;
import skillup_workspace.repository.EnrollmentRepository;
import skillup_workspace.repository.UserRepository;
import skillup_workspace.security.JwtService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
                "jwt.expiration=86400000",
                "r2.enabled=false"
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
    private CourseFavoriteRepository courseFavoriteRepository;

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
        courseFavoriteRepository.deleteAll();
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
    void favoriteCanBeReadAddedAndRemovedIdempotently() throws Exception {
        Course course = saveCourse();
        String studentToken = jwtService.generateToken(student);
        String path = "/api/v1/courses/" + course.getId() + "/favorite";

        HttpResponse<String> initialStatus = send(HttpMethod.GET, path, studentToken, null);
        assertEquals(200, initialStatus.statusCode());
        assertTrue(initialStatus.body().contains("\"favorited\":false"));

        assertEquals(204, send(HttpMethod.PUT, path, studentToken, null).statusCode());
        assertEquals(204, send(HttpMethod.PUT, path, studentToken, null).statusCode());
        assertEquals(1, courseFavoriteRepository.count());

        HttpResponse<String> savedStatus = send(HttpMethod.GET, path, studentToken, null);
        assertEquals(200, savedStatus.statusCode());
        assertTrue(savedStatus.body().contains("\"favorited\":true"));

        assertEquals(204, send(HttpMethod.DELETE, path, studentToken, null).statusCode());
        assertEquals(204, send(HttpMethod.DELETE, path, studentToken, null).statusCode());
        assertEquals(0, courseFavoriteRepository.count());
    }

    @Test
    void deletingCourseAlsoRemovesItsFavorites() throws Exception {
        Course course = saveCourse();
        String studentToken = jwtService.generateToken(student);
        String favoritePath = "/api/v1/courses/" + course.getId() + "/favorite";
        assertEquals(204, send(HttpMethod.PUT, favoritePath, studentToken, null).statusCode());

        HttpResponse<String> response = delete(course.getId(), jwtService.generateToken(admin));

        assertEquals(204, response.statusCode());
        assertEquals(0, courseFavoriteRepository.count());
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
        assertTrue(response.body().contains("\"createdAt\":"));
        Long createdCourseId = courseRepository.findAll().get(0).getId();
        assertNotNull(jdbcTemplate.queryForObject(
                "select created_at from courses where course_id = ?",
                java.sql.Timestamp.class,
                createdCourseId
        ));
    }

    @Test
    void creatingDraftAutomaticallyPersistsCreatedAt() throws Exception {
        String draftRequest = COURSE_REQUEST.replace(
                "\"modality\": {}",
                "\"modality\": {}, \"status\": \"BORRADOR\""
        );
        HttpResponse<String> response = send(
                HttpMethod.POST,
                "/api/v1/courses",
                jwtService.generateToken(admin),
                draftRequest
        );

        assertEquals(201, response.statusCode());
        Course draft = courseRepository.findAll().get(0);
        assertEquals(CourseStatus.BORRADOR, draft.getStatus());
        assertNotNull(draft.getCreatedAt());
        assertTrue(response.body().contains("\"createdAt\":"));
    }

    @Test
    void publishingDraftRefreshesPersistedCreatedAt() throws Exception {
        Course course = saveCourse();
        LocalDateTime previousCreatedAt = LocalDateTime.now().minusDays(1);
        course.setStatus(CourseStatus.BORRADOR);
        course.setIsActive(false);
        course.setCreatedAt(previousCreatedAt);
        courseRepository.saveAndFlush(course);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/courses/" + course.getId() + "/status"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(admin))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"status\":\"PUBLICADO\"}"))
                .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        Course published = courseRepository.findById(course.getId()).orElseThrow();
        assertEquals(CourseStatus.PUBLICADO, published.getStatus());
        assertTrue(published.getCreatedAt().isAfter(previousCreatedAt));
        assertTrue(response.body().contains("\"createdAt\":"));
    }

    @Test
    void publicCourseListIncludesCreatedAtAndActiveEnrollmentCount() throws Exception {
        Course course = saveCourse();
        Enrollment activeEnrollment = new Enrollment();
        activeEnrollment.setCourse(course);
        activeEnrollment.setUser(student);
        activeEnrollment.setStatus("ACTIVE");
        enrollmentRepository.save(activeEnrollment);

        User anotherStudent = saveUser("inactive-student@example.com", "ALUMNO");
        Enrollment inactiveEnrollment = new Enrollment();
        inactiveEnrollment.setCourse(course);
        inactiveEnrollment.setUser(anotherStudent);
        inactiveEnrollment.setStatus("COMPLETED");
        enrollmentRepository.saveAndFlush(inactiveEnrollment);

        HttpResponse<String> response = send(HttpMethod.GET, "/api/v1/courses", null, null);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"createdAt\":"));
        assertTrue(response.body().contains("\"activeEnrollmentCount\":1"));
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

    @Test
    void studentEnrollmentCountExcludesAdminEnrollmentsAndCountsEachEnrollment() throws Exception {
        Course firstCourse = saveCourse();
        Course secondCourse = new Course();
        secondCourse.setTitle("Segundo curso");
        secondCourse.setSummary("Resumen");
        secondCourse.setDescription("Descripción");
        secondCourse.setCategory("Programación");
        secondCourse.setLevel("Inicial");
        secondCourse.setDurationHours(8);
        secondCourse.setDurationWeeks(2);
        secondCourse = courseRepository.saveAndFlush(secondCourse);

        Enrollment studentEnrollmentOne = new Enrollment();
        studentEnrollmentOne.setUser(student);
        studentEnrollmentOne.setCourse(firstCourse);
        enrollmentRepository.save(studentEnrollmentOne);

        Enrollment studentEnrollmentTwo = new Enrollment();
        studentEnrollmentTwo.setUser(student);
        studentEnrollmentTwo.setCourse(secondCourse);
        enrollmentRepository.save(studentEnrollmentTwo);

        Enrollment adminEnrollment = new Enrollment();
        adminEnrollment.setUser(admin);
        adminEnrollment.setCourse(firstCourse);
        enrollmentRepository.saveAndFlush(adminEnrollment);

        assertEquals(2L, enrollmentRepository.countByStudentRole());

        HttpResponse<String> response = send(
                HttpMethod.GET,
                "/api/v1/admin/dashboard-stats",
                jwtService.generateToken(admin),
                null
        );
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"totalCourses\":2"));
        assertTrue(response.body().contains("\"totalEnrollments\":2"));
        assertTrue(response.body().contains("\"completedCourses\":null"));
        assertTrue(response.body().contains("\"completedCoursesAvailable\":false"));
    }

    @Test
    void studentCannotReadAdminDashboardStats() throws Exception {
        HttpResponse<String> response = send(
                HttpMethod.GET,
                "/api/v1/admin/dashboard-stats",
                jwtService.generateToken(student),
                null
        );

        assertEquals(403, response.statusCode());
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
            String boundary = "SkillUpBoundary7MA4YWxkTrZu0gW";
            String multipartBody = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"course\"\r\n"
                    + "Content-Type: application/json\r\n\r\n"
                    + body + "\r\n"
                    + "--" + boundary + "--\r\n";
            request.header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE + "; boundary=" + boundary)
                    .method(method.name(), HttpRequest.BodyPublishers.ofString(multipartBody));
        } else {
            request.method(method.name(), HttpRequest.BodyPublishers.noBody());
        }
        return HTTP_CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}

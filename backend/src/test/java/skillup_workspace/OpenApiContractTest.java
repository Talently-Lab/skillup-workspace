package skillup_workspace;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;
import skillup_workspace.controller.AuthController;
import skillup_workspace.controller.CourseController;
import skillup_workspace.dto.request.CourseRequestDTO;
import skillup_workspace.dto.request.LoginRequest;
import skillup_workspace.dto.request.RegisterRequest;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiContractTest {

    @Test
    void courseCreationSchemaRequiresAllContractFields() {
        Map<String, Schema> schemas = ModelConverters.getInstance().read(CourseRequestDTO.class);
        Schema<?> courseSchema = schemas.get("CourseRequestDTO");

        assertNotNull(courseSchema);
        assertTrue(courseSchema.getRequired().containsAll(List.of(
                "title",
                "summary",
                "description",
                "category",
                "level",
                "durationHours",
                "durationWeeks",
                "modality"
        )));
    }

    @Test
    void registrationDocumentsConflictForDuplicateEmail() throws NoSuchMethodException {
        Method register = AuthController.class.getMethod("register", RegisterRequest.class);
        ApiResponses responses = register.getAnnotation(ApiResponses.class);
        Map<String, ApiResponse> responsesByCode = Arrays.stream(responses.value())
                .collect(Collectors.toMap(ApiResponse::responseCode, response -> response));

        assertTrue(responsesByCode.containsKey("201"));
        assertEquals("Datos de entrada inválidos", responsesByCode.get("400").description());
        assertEquals(
                "El correo electrónico ya está registrado",
                responsesByCode.get("409").description()
        );
    }

    @Test
    void loginDocumentsValidationAndInvalidCredentialsResponses() throws NoSuchMethodException {
        Method login = AuthController.class.getMethod("login", LoginRequest.class);
        Set<String> responseCodes = Arrays.stream(login.getAnnotation(ApiResponses.class).value())
                .map(ApiResponse::responseCode)
                .collect(Collectors.toSet());

        assertTrue(responseCodes.containsAll(Set.of("200", "400", "401")));
    }

    @Test
    void courseDeletionDocumentsSuccessAndMissingCourse() throws NoSuchMethodException {
        Method delete = CourseController.class.getMethod("deleteCourse", Long.class);
        Set<String> responseCodes = Arrays.stream(delete.getAnnotation(ApiResponses.class).value())
                .map(ApiResponse::responseCode)
                .collect(Collectors.toSet());

        assertTrue(responseCodes.containsAll(Set.of("204", "401", "403", "404", "409")));
    }

    @Test
    void courseMutationsDocumentAuthenticationAndAuthorizationResponses() throws NoSuchMethodException {
        List<Method> mutations = List.of(
                CourseController.class.getMethod("createCourse", CourseRequestDTO.class),
                CourseController.class.getMethod("updateCourse", Long.class, CourseRequestDTO.class),
                CourseController.class.getMethod("deleteCourse", Long.class)
        );

        for (Method mutation : mutations) {
            Set<String> responseCodes = Arrays.stream(mutation.getAnnotation(ApiResponses.class).value())
                    .map(ApiResponse::responseCode)
                    .collect(Collectors.toSet());
            assertTrue(responseCodes.containsAll(Set.of("401", "403")), mutation.getName());
        }
    }
}

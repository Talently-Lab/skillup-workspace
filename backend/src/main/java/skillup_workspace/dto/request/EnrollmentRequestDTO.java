package skillup_workspace.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EnrollmentRequestDTO {
    @NotNull(message = "El ID del curso es obligatorio")
    private Long courseId;
}
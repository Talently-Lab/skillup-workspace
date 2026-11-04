package skillup_workspace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import skillup_workspace.entity.CourseStatus;

@Getter
@Setter
@NoArgsConstructor
public class CourseStatusUpdateRequestDTO {

    @NotNull(message = "El estado del curso es obligatorio")
    @Schema(
            description = "Estado persistente del curso",
            allowableValues = {"PUBLICADO", "BORRADOR", "ARCHIVADO"},
            example = "PUBLICADO"
    )
    private CourseStatus status;
}

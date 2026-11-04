package skillup_workspace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleRequestDTO {

    private String days;
    private String startTime;
    private String endTime;

    @Schema(description = "Duración total exacta en minutos")
    @PositiveOrZero
    private Integer totalDurationMinutes;

    @Schema(description = "Duración estimada del contenido grabado en minutos")
    @PositiveOrZero
    private Integer recordedContentMinutes;
}
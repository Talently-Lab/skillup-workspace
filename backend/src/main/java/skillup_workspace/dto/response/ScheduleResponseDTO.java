package skillup_workspace.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleResponseDTO {

    private String days;
    private String startTime;
    private String endTime;
    @Schema(description = "Duración total exacta en minutos")
    private Integer totalDurationMinutes;
    @Schema(description = "Duración estimada del contenido grabado en minutos")
    private Integer recordedContentMinutes;
}
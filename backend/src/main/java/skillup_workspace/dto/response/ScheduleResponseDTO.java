package skillup_workspace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleResponseDTO {
    private String days;
    private String startTime;
    private String endTime;
}
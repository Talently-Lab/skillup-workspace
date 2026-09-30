package skillup_workspace.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleRequestDTO {
    private String days;
    private String startTime;
    private String endTime;
}
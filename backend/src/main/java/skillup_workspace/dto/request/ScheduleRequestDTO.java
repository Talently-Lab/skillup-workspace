package skillup_workspace.dto.request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleRequestDTO {

    private String days;
    private String startTime;
    private String endTime;
}
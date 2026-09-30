package skillup_workspace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponseDTO {
    private Integer id;
    private String title;
    private String summary;
    private String imageUrl;
    private String category;
    private String level;
    private Integer durationHours;
    private LocalDate nextStartDate;
    private ScheduleResponseDTO schedule;
}
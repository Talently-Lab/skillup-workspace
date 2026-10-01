package skillup_workspace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponseDTO {

    private Long id;
    private String title;
    private String summary;
    private String imageUrl;
    private String category;
    private String level;
    private Integer durationHours;
    private LocalDate nextStartDate;
    private ScheduleResponseDTO schedule;
}
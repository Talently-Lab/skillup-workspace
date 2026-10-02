package skillup_workspace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseDetailResponseDTO {

    private Long id;
    private String title;
    private String summary;
    private String description;
    private String imageUrl;
    private String category;
    private String level;
    private Integer durationHours;
    private Integer durationWeeks;
    private LocalDate nextStartDate;
    private ScheduleResponseDTO schedule;
    private ModalityResponseDTO modality;
    private String syllabusUrl;
    private List<String> learningGoals;
    private List<String> objectives;
    private List<ContentModuleResponseDTO> contentModules;
    private Boolean isActive;
}
package skillup_workspace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseRequestDTO {

    private String title;

    private String summary;

    private String description;

    private String imageUrl;

    private String category;

    private String level;


    private Integer durationHours;

    private Integer durationWeeks;

    private LocalDate nextStartDate;

    private ScheduleRequestDTO schedule;

    private ModalityRequestDTO modality;

    private String syllabusUrl;

    private List<String> learningGoals;

    private List<String> objectives;

    private List<ContentModuleRequestDTO> contentModules;
}
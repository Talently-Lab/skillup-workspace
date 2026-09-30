package skillup_workspace.dto.response;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class CourseDetailResponseDTO {
    private Integer id;
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
    private InstructorResponseDTO instructor;
    private Boolean isActive;
}
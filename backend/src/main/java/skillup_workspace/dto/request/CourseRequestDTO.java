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

    @NotBlank(message = "El campo title es obligatorio")
    private String title;

    @NotBlank(message = "El campo summary es obligatorio")
    private String summary;

    @NotBlank(message = "El campo description es obligatorio")
    private String description;

    @NotBlank(message = "El campo category es obligatorio")
    private String category;

    @NotBlank(message = "El campo level es obligatorio")
    private String level;

    @NotNull(message = "La duración en horas es obligatoria")
    @Positive(message = "La duración en horas debe ser mayor a cero")
    private Integer durationHours;

    @NotNull(message = "La duración en semanas es obligatoria")
    @Positive(message = "La duración en semanas debe ser mayor a cero")
    private Integer durationWeeks;

    @NotNull(message = "La modalidad es obligatoria")
    private ModalityRequestDTO modality;

    private String imageUrl;

    private LocalDate nextStartDate;

    private ScheduleRequestDTO schedule;

    private String syllabusUrl;

    private List<String> learningGoals;

    private List<String> objectives;

    private List<ContentModuleRequestDTO> contentModules;
}
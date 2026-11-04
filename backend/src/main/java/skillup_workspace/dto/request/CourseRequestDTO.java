package skillup_workspace.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import skillup_workspace.entity.CourseStatus;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(example = """
        {
          "title": "Diseño UX/UI Avanzado",
          "summary": "Aprendé los fundamentos del diseño de experiencias digitales y creá interfaces centradas en el usuario.",
          "description": "Este curso te va a permitir comprender el proceso de diseño desde la investigación hasta la creación de prototipos.",
          "category": "Diseño UX/UI",
          "level": "Avanzado",
          "durationHours": 40,
          "durationWeeks": 14,
          "nextStartDate": "2026-11-01",
          "schedule": {
            "days": "Martes y Jueves",
            "startTime": "18:00",
            "endTime": "20:00"
          },
          "modality": {
            "type": "EN VIVO",
            "frequency": "2 clases semanales",
            "durationPerClass": "2hs"
          },
          "learningGoals": [
            "Comprender las necesidades del usuario.",
            "Diseñar interfaces funcionales y atractivas.",
            "Crear prototipos interactivos complejos."
          ],
          "objectives": [
            "Dominar metodologías de investigación con usuarios.",
            "Aprender a diseñar Wireframes y Prototipos en Figma."
          ],
          "contentModules": [
            {
              "moduleNumber": 1,
              "title": "Introducción al Diseño UX",
              "topics": [
                "¿Qué es UX/UI?",
                "Metodología Design Thinking"
              ]
            }
          ]
        }
        """)
public class CourseRequestDTO {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "El campo title es obligatorio")
    private String title;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "El campo summary es obligatorio")
    private String summary;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "El campo description es obligatorio")
    private String description;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "El campo category es obligatorio")
    private String category;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "El campo level es obligatorio")
    private String level;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "La duración en horas es obligatoria")
    @Positive(message = "La duración en horas debe ser mayor a cero")
    private Integer durationHours;

    @Schema(description = "Duración estimada; opcional para cursos grabados")
    private Integer durationWeeks;

    @Schema(
            description = "Estado inicial del curso; si se omite, se crea publicado",
            allowableValues = {"PUBLICADO", "BORRADOR", "ARCHIVADO"},
            example = "BORRADOR"
    )
    private CourseStatus status;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "La modalidad es obligatoria")
    private ModalityRequestDTO modality;

    @Schema(type = "string", format = "date", example = "2026-11-07")
    @JsonDeserialize(using = IsoLocalDateDeserializer.class)
    private LocalDate nextStartDate;

    @Valid
    private ScheduleRequestDTO schedule;

    private List<String> learningGoals;

    private List<String> objectives;

    private List<ContentModuleRequestDTO> contentModules;

    @AssertTrue(message = "La duración en semanas debe ser mayor a cero, excepto en cursos grabados")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isDurationWeeksValid() {
        boolean recordedCourse = modality != null
                && modality.getType() != null
                && modality.getType().trim().equalsIgnoreCase("GRABADO");
        return recordedCourse
                ? durationWeeks == null || durationWeeks > 0
                : durationWeeks != null && durationWeeks > 0;
    }
}
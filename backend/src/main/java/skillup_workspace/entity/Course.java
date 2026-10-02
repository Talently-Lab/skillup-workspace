package skillup_workspace.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import skillup_workspace.dto.request.ContentModuleRequestDTO;
import skillup_workspace.dto.request.ModalityRequestDTO;
import skillup_workspace.dto.request.ScheduleRequestDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    private String category;
    private String level;

    @Column(name = "duration_hours")
    private Integer durationHours;

    @Column(name = "duration_weeks")
    private Integer durationWeeks;

    @Column(name = "next_start_date")
    private LocalDate nextStartDate;

    @Column(name = "syllabus_url")
    private String syllabusUrl;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schedule", columnDefinition = "json")
    private ScheduleRequestDTO schedule;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "modality", columnDefinition = "json")
    private ModalityRequestDTO modality;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "learning_goals", columnDefinition = "json")
    private List<String> learningGoals;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "objectives", columnDefinition = "json")
    private List<String> objectives;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_modules", columnDefinition = "json")
    private List<ContentModuleRequestDTO> contentModules;
}
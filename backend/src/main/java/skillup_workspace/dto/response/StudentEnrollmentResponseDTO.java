package skillup_workspace.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudentEnrollmentResponseDTO {
    private Long id;
    private Long courseId;
    private String courseTitle;
    private String summary;
    private String imageUrl;
    private String category;
    private String level;
    private Integer durationHours;
    private Integer durationWeeks;
    private ScheduleResponseDTO schedule;
    private String status;
    private LocalDateTime enrolledAt;
}

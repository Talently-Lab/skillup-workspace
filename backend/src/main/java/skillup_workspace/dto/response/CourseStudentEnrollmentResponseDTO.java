package skillup_workspace.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseStudentEnrollmentResponseDTO {
    private Long enrollmentId;
    private Long studentId;
    private String firstName;
    private String lastName;
    private String email;
    private String status;
    private LocalDateTime enrolledAt;
}

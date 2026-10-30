package skillup_workspace.dto.response;

import lombok.Data;

@Data
public class EnrollmentResponseDTO {
    private Long id;
    private Long courseId;
    private String courseTitle;
    private String status;
}
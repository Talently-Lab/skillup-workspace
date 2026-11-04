package skillup_workspace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminDashboardStatsResponseDTO {

    private long totalCourses;
    private long totalEnrollments;
    private Long completedCourses;
    private boolean completedCoursesAvailable;
}

package skillup_workspace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CourseFavoriteResponseDTO {
    private Long courseId;
    private boolean favorited;
}

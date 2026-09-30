package skillup_workspace.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstructorRequestDTO {
    private String name;
    private String title;
    private String bio;
    private String imageUrl;
}
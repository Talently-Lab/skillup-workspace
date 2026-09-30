package skillup_workspace.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class ContentModuleResponseDTO {
    private Integer moduleNumber;
    private String title;
    private List<String> topics;
}
package skillup_workspace.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentModuleRequestDTO {
    private Integer moduleNumber;
    private String title;
    private List<String> topics;
}
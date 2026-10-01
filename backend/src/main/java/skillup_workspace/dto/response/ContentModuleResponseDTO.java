package skillup_workspace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContentModuleResponseDTO {

    private Integer moduleNumber;
    private String title;
    private List<String> topics;
}
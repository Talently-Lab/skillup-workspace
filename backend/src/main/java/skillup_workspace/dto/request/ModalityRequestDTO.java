package skillup_workspace.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModalityRequestDTO {
    private String type;
    private String frequency;
    private String durationPerClass;
}
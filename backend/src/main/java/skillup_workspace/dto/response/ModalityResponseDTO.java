package skillup_workspace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ModalityResponseDTO {

    private String type;
    private String frequency;
    private String durationPerClass;
}
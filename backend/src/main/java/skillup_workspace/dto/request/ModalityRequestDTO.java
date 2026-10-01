package skillup_workspace.dto.request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModalityRequestDTO {

    private String type;
    private String frequency;
    private String durationPerClass;
}
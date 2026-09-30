package skillup_workspace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Modality {

    @Column(name = "modality_type")
    private String type;

    @Column(name = "modality_frequency")
    private String frequency;

    @Column(name = "modality_duration_per_class")
    private String durationPerClass;
}
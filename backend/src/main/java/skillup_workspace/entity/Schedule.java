package skillup_workspace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Schedule {
    @Column(name = "schedule_days")
    private String days;
    @Column(name = "schedule_start_time")
    private LocalTime startTime;

    @Column(name = "schedule_end_time")
    private LocalTime endTime;
}

package skillup_workspace.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "content_modules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "module_id")
    private Integer moduleId;

    @Column(name = "module_number")
    private Integer moduleNumber;

    private String title;

    @ElementCollection
    @CollectionTable(name = "module_topics", joinColumns = @JoinColumn(name = "module_id"))
    @Column(name = "topic")
    private List<String> topics;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;
}
package skillup_workspace.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Integer courseId;

    @Column(nullable = false)
    private String title;

    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    private String category;

    private String level;

    @Column(name = "duration_hours")
    private Integer durationHours;

    @Column(name = "duration_weeks")
    private Integer durationWeeks;

    @Column(name = "next_start_date")
    private LocalDate nextStartDate;

    @Embedded
    private Schedule schedule;

    @Embedded
    private Modality modality;

    @Column(name = "syllabus_url")
    private String syllabusUrl;

    @ElementCollection
    @CollectionTable(name = "course_learning_goals", joinColumns = @JoinColumn(name = "course_id"))
    @Column(name = "learning_goal")
    private List<String> learningGoals = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "course_objectives", joinColumns = @JoinColumn(name = "course_id"))
    @Column(name = "objective")
    private List<String> objectives = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContentModule> contentModules = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id")
    private Instructor instructor;

    @Column(name = "is_active")
    private Boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
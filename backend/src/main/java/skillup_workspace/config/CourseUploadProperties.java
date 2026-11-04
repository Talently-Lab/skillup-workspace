package skillup_workspace.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "course.upload")
public class CourseUploadProperties {

    private int maxConcurrentTasks = 2;
    private int queueCapacity = 8;
    private long requestWaitTimeoutMs = 10_000;

    @PostConstruct
    void validate() {
        if (maxConcurrentTasks < 1) {
            throw new IllegalStateException("course.upload.max-concurrent-tasks debe ser al menos 1.");
        }
        if (queueCapacity < 1) {
            throw new IllegalStateException("course.upload.queue-capacity debe ser al menos 1.");
        }
        if (requestWaitTimeoutMs < 1) {
            throw new IllegalStateException("course.upload.request-wait-timeout-ms debe ser mayor que cero.");
        }
    }
}

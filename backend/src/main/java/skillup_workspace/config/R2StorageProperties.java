package skillup_workspace.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "r2")
public class R2StorageProperties {

    private boolean enabled;
    private String accountId;
    private String accessKeyId;
    private String secretAccessKey;
    private String bucketName = "skillupcampus";
    private String publicUrl;
    private int presignedUrlDurationSeconds = 300;
    private long maxImageSizeBytes = 5 * 1024 * 1024;
    private long maxPdfSizeBytes = 20 * 1024 * 1024;
}

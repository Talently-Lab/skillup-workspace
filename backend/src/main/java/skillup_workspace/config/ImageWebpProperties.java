package skillup_workspace.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "image.webp")
public class ImageWebpProperties {

    private boolean enabled = true;
    private float quality = 0.80f;
    private long maxPixels = 20_000_000L;
}

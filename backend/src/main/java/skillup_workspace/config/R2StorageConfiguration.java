package skillup_workspace.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@ConditionalOnProperty(prefix = "r2", name = "enabled", havingValue = "true")
public class R2StorageConfiguration {

    @Bean
    @Lazy
    public S3Client r2S3Client(R2StorageProperties properties) {
        validateCredentials(properties);
        return S3Client.builder()
                .endpointOverride(endpoint(properties))
                .region(Region.of("auto"))
                .credentialsProvider(credentials(properties))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    @Bean
    @Lazy
    public S3Presigner r2S3Presigner(R2StorageProperties properties) {
        validateCredentials(properties);
        return S3Presigner.builder()
                .endpointOverride(endpoint(properties))
                .region(Region.of("auto"))
                .credentialsProvider(credentials(properties))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    private URI endpoint(R2StorageProperties properties) {
        return URI.create("https://" + properties.getAccountId() + ".r2.cloudflarestorage.com");
    }

    private StaticCredentialsProvider credentials(R2StorageProperties properties) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.getAccessKeyId(), properties.getSecretAccessKey())
        );
    }

    private void validateCredentials(R2StorageProperties properties) {
        if (isBlank(properties.getAccountId())
                || isBlank(properties.getAccessKeyId())
                || isBlank(properties.getSecretAccessKey())
                || isBlank(properties.getBucketName())) {
            throw new IllegalStateException(
                    "R2_ENABLED=true requiere R2_ACCOUNT_ID, R2_ACCESS_KEY_ID, "
                            + "R2_SECRET_ACCESS_KEY y R2_BUCKET_NAME en el entorno."
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

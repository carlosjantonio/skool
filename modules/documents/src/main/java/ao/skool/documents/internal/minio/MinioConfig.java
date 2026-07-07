package ao.skool.documents.internal.minio;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);
    private final MinioProperties props;

    public MinioConfig(MinioProperties props) {
        this.props = props;
    }

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(props.endpoint())
                .credentials(props.accessKey(), props.secretKey())
                .build();
    }

    /**
     * Ensure the target bucket exists once the context is fully wired. Deliberately runs
     * on {@link ApplicationReadyEvent} rather than {@code @PostConstruct} so it doesn't
     * try to resolve the {@code MinioClient} bean before its own configuration is
     * initialised.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void ensureBucket(ApplicationReadyEvent event) {
        MinioClient client = event.getApplicationContext().getBean(MinioClient.class);
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(props.bucket()).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(props.bucket()).build());
                log.info("Created MinIO bucket: {}", props.bucket());
            } else {
                log.info("MinIO bucket present: {}", props.bucket());
            }
        } catch (Exception e) {
            log.warn("MinIO not reachable at {}: {}. Uploads will fail until it's up.",
                    props.endpoint(), e.getMessage());
        }
    }
}

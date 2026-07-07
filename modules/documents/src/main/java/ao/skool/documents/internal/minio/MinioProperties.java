package ao.skool.documents.internal.minio;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "skool.storage.minio")
public record MinioProperties(
        String endpoint,
        String accessKey,
        String secretKey,
        String bucket
) {
    public MinioProperties {
        if (endpoint == null || endpoint.isBlank()) endpoint = "http://localhost:9000";
        if (bucket == null || bucket.isBlank()) bucket = "skool";
    }
}

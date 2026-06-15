package io.marcus.infrastructure.adapter.storage;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "minio")
@Getter
@Setter
@Slf4j
public class MinioConfig {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;

    @Bean
    public MinioClient minioClient() {
        log.info("Initializing MinIO Client connecting to: {}", endpoint);
        MinioClient minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();

        try {
            boolean found = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build()
            );
            if (!found) {
                log.info("Bucket '{}' not found, creating it...", bucket);
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucket).build()
                );
                log.info("Bucket '{}' created successfully.", bucket);
            } else {
                log.info("Bucket '{}' already exists.", bucket);
            }
        } catch (Exception e) {
            log.error("Failed to check/create MinIO bucket: {}", bucket, e);
        }

        return minioClient;
    }
}

package io.marcus.infrastructure.adapter.storage;

import io.marcus.domain.port.StoragePort;
import io.marcus.domain.vo.ObjectMetadata;
import io.marcus.domain.vo.PresignedPostData;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PostPolicy;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class MinioStorageAdapter implements StoragePort {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Override
    public PresignedPostData generatePresignedPostUploadUrl(String objectKey, String contentType, long minSize, long maxSize) {
        try {
            // Policy expires in 15 minutes
            ZonedDateTime expiration = ZonedDateTime.now().plusMinutes(15);
            PostPolicy policy = new PostPolicy(minioConfig.getBucket(), expiration);
            
            // Add exact conditions
            policy.addEqualsCondition("key", objectKey);
            policy.addEqualsCondition("Content-Type", contentType);
            policy.addContentLengthRangeCondition(minSize, maxSize);

            Map<String, String> rawFormData = minioClient.getPresignedPostFormData(policy);
            
            String uploadUrl = rawFormData.get("form_action");
            
            // Prepare client-facing form fields
            Map<String, String> clientFormData = new HashMap<>(rawFormData);
            clientFormData.remove("form_action");
            
            // S3 POST standard requires key and Content-Type to be sent by client. 
            // We append these to formData so client doesn't need to construct them.
            clientFormData.put("key", objectKey);
            clientFormData.put("Content-Type", contentType);

            return PresignedPostData.builder()
                    .uploadUrl(uploadUrl)
                    .formData(clientFormData)
                    .build();
        } catch (Exception e) {
            log.error("Failed to generate presigned POST URL for key: {}", objectKey, e);
            throw new RuntimeException("Failed to generate upload URL", e);
        }
    }

    @Override
    public ObjectMetadata getObjectMetadata(String objectKey) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(objectKey)
                            .build()
            );
            return ObjectMetadata.builder()
                    .exists(true)
                    .size(stat.size())
                    .contentType(stat.contentType())
                    .build();
        } catch (ErrorResponseException e) {
            // Code NoSuchKey (S3 standard) indicates object does not exist
            if ("NoSuchKey".equals(e.errorResponse().code()) || "NoSuchBucket".equals(e.errorResponse().code())) {
                return ObjectMetadata.builder().exists(false).build();
            }
            log.error("Error stat-ing object: {}", objectKey, e);
            throw new RuntimeException("Failed to check storage object metadata", e);
        } catch (Exception e) {
            log.error("Failed to retrieve object metadata for key: {}", objectKey, e);
            throw new RuntimeException("Failed to retrieve storage object metadata", e);
        }
    }

    @Override
    public byte[] fetchFirstNBytes(String objectKey, int bytesCount) {
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioConfig.getBucket())
                        .object(objectKey)
                        .offset(0L)
                        .length((long) bytesCount)
                        .build())) {
            return stream.readAllBytes();
        } catch (Exception e) {
            log.error("Failed to fetch first {} bytes of key: {}", bytesCount, objectKey, e);
            throw new RuntimeException("Failed to read object content from storage", e);
        }
    }
}

package io.marcus.domain.port;

import io.marcus.domain.vo.ObjectMetadata;
import io.marcus.domain.vo.PresignedPostData;

/**
 * Port interface for interacting with Object Storage (MinIO).
 * Handles pre-signed POST policy generation, metadata checking, and byte fetching.
 */
public interface StoragePort {

    /**
     * Generates cryptographic pre-signed POST form data for client-direct uploads.
     * Enforces content-length range and content-type constraints directly at storage edge.
     *
     * @param objectKey unique key/path in the bucket
     * @param contentType expected file MIME type
     * @param minSize minimum allowable file size in bytes
     * @param maxSize maximum allowable file size in bytes
     * @return pre-signed POST URL and required form fields
     */
    PresignedPostData generatePresignedPostUploadUrl(String objectKey, String contentType, long minSize, long maxSize);

    /**
     * Retrieves object metadata from MinIO.
     *
     * @param objectKey unique key/path in the bucket
     * @return object metadata (existence, size, content type)
     */
    ObjectMetadata getObjectMetadata(String objectKey);

    /**
     * Fetches the first N bytes of an object to verify file signature/magic bytes.
     *
     * @param objectKey unique key/path in the bucket
     * @param bytesCount number of bytes to retrieve
     * @return byte array containing the requested range
     */
    byte[] fetchFirstNBytes(String objectKey, int bytesCount);
}

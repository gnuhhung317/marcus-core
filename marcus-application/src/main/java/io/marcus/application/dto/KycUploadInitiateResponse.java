package io.marcus.application.dto;

import io.marcus.domain.vo.PresignedPostData;

public record KycUploadInitiateResponse(
        String documentId,
        String objectKey,
        PresignedPostData presignedPostData
) {
}

package io.marcus.domain.vo;

import lombok.Builder;
import lombok.Value;
import java.util.Map;

@Value
@Builder
public class PresignedPostData {
    String uploadUrl;
    Map<String, String> formData;
}

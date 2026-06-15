package io.marcus.domain.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ObjectMetadata {
    boolean exists;
    long size;
    String contentType;
}

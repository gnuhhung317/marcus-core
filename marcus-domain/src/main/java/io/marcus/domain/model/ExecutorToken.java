package io.marcus.domain.model;

import io.marcus.domain.vo.ExecutorTokenStatus;

import java.time.LocalDateTime;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ExecutorToken extends BaseModel{
    private String executorTokenId;
    private String userId;
    private String name;
    private ExecutorTokenStatus status;
    @Builder.Default
    private LocalDateTime lastConnected = LocalDateTime.now();
}

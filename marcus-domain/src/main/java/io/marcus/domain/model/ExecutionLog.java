package io.marcus.domain.model;
import io.marcus.domain.vo.ExecutionStatus;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ExecutionLog extends BaseModel {
    private String executionLogId;
    private String signalId;
    private String userId;
    private ExecutionStatus status;
}

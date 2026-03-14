package io.marcus.domain.model;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ExecutionLog extends BaseModel {
    private String executionLogId;
    private String signalId;
    private String userId;
    private ExecutionStatus status;
}

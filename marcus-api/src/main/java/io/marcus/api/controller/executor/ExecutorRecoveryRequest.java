package io.marcus.api.controller.executor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for executor recovery endpoint.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutorRecoveryRequest {
    
    /**
     * List of signal IDs to recover.
     */
    private List<String> signalIds;
}

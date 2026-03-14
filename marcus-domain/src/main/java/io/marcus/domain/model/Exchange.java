package io.marcus.domain.model;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)

public class Exchange extends BaseModel{
    private String exchangeId;
    private String name;
    private String baseUrl;
    private String isActive;
}

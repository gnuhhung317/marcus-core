package io.marcus.domain.model;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)

public class Exchange extends BaseModel{
    private String exchangeId;
    private String name;
    private String baseUrl;
    private String isActive;
}

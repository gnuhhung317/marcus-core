package io.marcus.domain.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder // Dùng SuperBuilder thay vì Builder để kế thừa được các trường này
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseModel {
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt; // Nếu null là chưa xóa, có giá trị là đã xóa
}
package com.zubair.taskpulse.dto.whatsapp;
import com.zubair.taskpulse.entity.TaskPriority;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
public record WhatsAppConfirmRequest(
        @NotBlank(message = "Title is required")
        String title,
        TaskPriority priority,
        LocalDateTime deadline,
        @Min(value = 1, message = "Estimated duration must be at least 1 minute")
        @Max(value = 1440, message = "Estimated duration cannot exceed 1440 minutes")
        Integer estimatedDurationMinutes
) {
}

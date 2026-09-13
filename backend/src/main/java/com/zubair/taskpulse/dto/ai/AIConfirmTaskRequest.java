package com.zubair.taskpulse.dto.ai;

import com.zubair.taskpulse.entity.TaskPriority;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public record AIConfirmTaskRequest(
        @NotBlank(message = "Title is required")
        String title,
        String description,
        TaskPriority priority,
        LocalDateTime deadline,
        @Min(value = 1, message = "Duration must be at least 1 minute")
        @Max(value = 1440, message = "Duration cannot exceed 1440 minutes")
        Integer estimatedDurationMinutes
) {
}

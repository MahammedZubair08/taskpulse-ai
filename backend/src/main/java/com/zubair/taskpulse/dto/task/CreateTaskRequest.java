package com.zubair.taskpulse.dto.task;

import com.zubair.taskpulse.entity.TaskPriority;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateTaskRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    private String description;

    private TaskPriority priority;

    @Future(message = "Deadline must be in the future")
    private LocalDateTime deadline;

    @Min(value = 1, message = "Estimated duration must be at least 1 minute")
    @Max(value = 1440, message = "Estimated duration cannot exceed 1440 minutes")
    private Integer estimatedDurationMinutes;
}
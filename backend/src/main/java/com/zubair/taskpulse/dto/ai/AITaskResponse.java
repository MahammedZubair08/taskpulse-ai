package com.zubair.taskpulse.dto.ai;

import com.zubair.taskpulse.entity.TaskPriority;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AITaskResponse(

        @NotBlank
        String title,

        TaskPriority priority,

        String deadlineExpression,

        @Min(1)
        @Max(1440)
        Integer estimatedDurationMinutes
) {
}
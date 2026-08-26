package com.zubair.taskpulse.dto.ai;

import jakarta.validation.constraints.NotBlank;

public record AITaskRequest(
        @NotBlank(message = "Prompt cannot be empty")
        String prompt
) {
}
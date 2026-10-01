package com.zubair.taskpulse.dto.chat;
import jakarta.validation.constraints.NotBlank;
public record ChatRequest(
        @NotBlank(message = "Message cannot be empty")
        String message
) {
}

package com.zubair.taskpulse.dto.whatsapp;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record WhatsAppConnectRequest(
        @NotBlank(message = "Phone number is required")
        @Size(max = 30, message = "Phone number cannot exceed 30 characters")
        String phoneNumber
) {
}

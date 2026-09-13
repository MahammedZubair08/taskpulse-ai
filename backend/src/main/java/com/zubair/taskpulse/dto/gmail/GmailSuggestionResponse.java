package com.zubair.taskpulse.dto.gmail;

import com.zubair.taskpulse.entity.TaskPriority;

import java.time.LocalDateTime;

public record GmailSuggestionResponse(
        String emailId,
        String emailSubject,
        String emailFrom,
        String emailSnippet,
        String title,
        TaskPriority priority,
        String deadlineExpression,
        LocalDateTime deadline,
        Integer estimatedDurationMinutes
) {}

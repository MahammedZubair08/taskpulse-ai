package com.zubair.taskpulse.dto.gmail;

import com.zubair.taskpulse.dto.task.TaskResponse;

import java.util.List;

public record GmailSyncResultResponse(
        List<TaskResponse> autoCreatedTasks,
        List<GmailSuggestionResponse> suggestions
) {}

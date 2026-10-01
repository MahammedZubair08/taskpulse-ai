package com.zubair.taskpulse.dto.calendar;

import java.time.LocalDateTime;

public record ScheduleConflictResponse(
        Long taskId,
        String taskTitle,
        LocalDateTime taskDeadline,
        String conflictingEventSummary,
        LocalDateTime eventStart,
        LocalDateTime eventEnd
) {}

package com.zubair.taskpulse.dto.calendar;

import java.time.LocalDateTime;

public record CalendarEventResponse(
        String id,
        String summary,
        String description,
        LocalDateTime start,
        LocalDateTime end,
        boolean allDay
) {}

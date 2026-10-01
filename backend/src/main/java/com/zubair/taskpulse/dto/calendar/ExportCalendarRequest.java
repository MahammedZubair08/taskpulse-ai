package com.zubair.taskpulse.dto.calendar;

import java.time.LocalDateTime;

public record ExportCalendarRequest(
        LocalDateTime startTime
) {}

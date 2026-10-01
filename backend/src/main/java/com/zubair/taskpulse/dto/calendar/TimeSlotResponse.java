package com.zubair.taskpulse.dto.calendar;

import java.time.LocalDateTime;

public record TimeSlotResponse(
        LocalDateTime start,
        LocalDateTime end,
        Integer durationMinutes
) {}

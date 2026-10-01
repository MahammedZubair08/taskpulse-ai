package com.zubair.taskpulse.dto.scheduling;

import java.time.LocalDate;
import java.util.List;

public record OptimizedScheduleResponse(
        LocalDate targetDate,
        List<ScheduledTaskBlock> schedule,
        String overallStrategy
) {}

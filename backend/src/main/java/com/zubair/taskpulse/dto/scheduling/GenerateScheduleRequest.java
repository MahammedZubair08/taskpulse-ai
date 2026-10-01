package com.zubair.taskpulse.dto.scheduling;

import java.time.LocalDate;

public record GenerateScheduleRequest(
        LocalDate targetDate
) {}

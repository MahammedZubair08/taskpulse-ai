package com.zubair.taskpulse.dto.scheduling;

import java.util.List;

public record ApplyScheduleRequest(
        List<ScheduledTaskBlock> schedule,
        boolean exportToGoogleCalendar
) {}

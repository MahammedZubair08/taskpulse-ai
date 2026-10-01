package com.zubair.taskpulse.dto.scheduling;

import com.zubair.taskpulse.entity.TaskPriority;

import java.time.LocalDateTime;

public record ScheduledTaskBlock(
        Long taskId,
        String taskTitle,
        TaskPriority priority,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer durationMinutes,
        String reasoning
) {}

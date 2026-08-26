package com.zubair.taskpulse.dto.task;

import com.zubair.taskpulse.entity.TaskPriority;
import com.zubair.taskpulse.entity.TaskStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TaskResponse {

    private Long id;

    private String title;

    private String description;

    private TaskStatus status;

    private TaskPriority priority;

    private LocalDateTime deadline;

    private Integer estimatedDurationMinutes;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
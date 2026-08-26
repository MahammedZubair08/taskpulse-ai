package com.zubair.taskpulse.dto.ai;

import com.zubair.taskpulse.entity.TaskPriority;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedTask {

    private String title;

    private String description;

    private TaskPriority priority;

    private LocalDateTime deadline;

    private Integer estimatedDurationMinutes;
}
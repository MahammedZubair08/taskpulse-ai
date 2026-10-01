package com.zubair.taskpulse.dto.chat;
import com.zubair.taskpulse.dto.task.TaskResponse;
import java.util.List;
public record ChatResponse(
        String message,
        List<TaskResponse> tasks
) {
}

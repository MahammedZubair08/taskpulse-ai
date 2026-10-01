package com.zubair.taskpulse.dto.notification;

import com.zubair.taskpulse.entity.Notification;
import com.zubair.taskpulse.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        Long taskId,
        String taskTitle,
        String title,
        String message,
        NotificationType type,
        boolean read,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
    public static NotificationResponse fromEntity(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTask() != null ? notification.getTask().getId() : null,
                notification.getTask() != null ? notification.getTask().getTitle() : null,
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}

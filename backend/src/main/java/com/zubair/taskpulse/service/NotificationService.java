package com.zubair.taskpulse.service;

import com.zubair.taskpulse.dto.notification.NotificationResponse;
import com.zubair.taskpulse.entity.NotificationType;
import com.zubair.taskpulse.entity.Task;
import com.zubair.taskpulse.entity.User;

import java.util.List;

public interface NotificationService {

    List<NotificationResponse> getNotificationsForUser(User user);

    long getUnreadCount(User user);

    NotificationResponse markAsRead(Long notificationId, User user);

    int markAllAsRead(User user);

    void deleteNotification(Long notificationId, User user);

    NotificationResponse createNotification(User user, Task task, String title, String message, NotificationType type);

    NotificationResponse generateDailySummary(User user);
}

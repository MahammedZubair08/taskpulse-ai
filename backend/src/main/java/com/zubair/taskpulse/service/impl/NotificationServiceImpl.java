package com.zubair.taskpulse.service.impl;

import com.zubair.taskpulse.dto.notification.NotificationResponse;
import com.zubair.taskpulse.entity.Notification;
import com.zubair.taskpulse.entity.NotificationType;
import com.zubair.taskpulse.entity.Task;
import com.zubair.taskpulse.entity.TaskStatus;
import com.zubair.taskpulse.entity.User;
import com.zubair.taskpulse.repository.NotificationRepository;
import com.zubair.taskpulse.repository.TaskRepository;
import com.zubair.taskpulse.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final TaskRepository taskRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsForUser(User user) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(NotificationResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserIdAndReadFalse(user.getId());
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, User user) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with id: " + notificationId));

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }
        return NotificationResponse.fromEntity(notification);
    }

    @Override
    @Transactional
    public int markAllAsRead(User user) {
        return notificationRepository.markAllAsReadByUserId(user.getId(), LocalDateTime.now());
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId, User user) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with id: " + notificationId));
        notificationRepository.delete(notification);
    }

    @Override
    @Transactional
    public NotificationResponse createNotification(User user, Task task, String title, String message, NotificationType type) {
        Notification notification = Notification.builder()
                .user(user)
                .task(task)
                .title(title)
                .message(message)
                .type(type)
                .read(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Created notification [ID: {}, Type: {}] for user: {}", saved.getId(), type, user.getEmail());
        return NotificationResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public NotificationResponse generateDailySummary(User user) {
        List<Task> tasks = taskRepository.findByUserId(user.getId());
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        List<Task> activeTasks = tasks.stream()
                .filter(t -> t.getStatus() != TaskStatus.COMPLETED && t.getStatus() != TaskStatus.CANCELLED)
                .toList();

        long overdueCount = activeTasks.stream()
                .filter(t -> t.getDeadline() != null && t.getDeadline().isBefore(now))
                .count();

        long todayCount = activeTasks.stream()
                .filter(t -> t.getDeadline() != null && t.getDeadline().toLocalDate().isEqual(today))
                .count();

        long highPriorityCount = activeTasks.stream()
                .filter(t -> t.getPriority() != null && (t.getPriority().name().equals("HIGH") || t.getPriority().name().equals("URGENT")))
                .count();

        StringBuilder summaryBuilder = new StringBuilder();
        summaryBuilder.append("Good day! Here is your task briefing:\n");
        summaryBuilder.append("• Active Tasks: ").append(activeTasks.size()).append("\n");
        summaryBuilder.append("• Due Today: ").append(todayCount).append("\n");
        summaryBuilder.append("• Overdue: ").append(overdueCount).append("\n");
        summaryBuilder.append("• High / Urgent Priority: ").append(highPriorityCount);

        String title = "Daily Task Summary — " + today.toString();
        return createNotification(user, null, title, summaryBuilder.toString(), NotificationType.DAILY_SUMMARY);
    }
}

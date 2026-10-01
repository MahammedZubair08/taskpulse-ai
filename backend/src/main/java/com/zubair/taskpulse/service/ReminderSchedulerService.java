package com.zubair.taskpulse.service;

import com.zubair.taskpulse.entity.NotificationType;
import com.zubair.taskpulse.entity.Task;
import com.zubair.taskpulse.entity.TaskStatus;
import com.zubair.taskpulse.entity.User;
import com.zubair.taskpulse.repository.NotificationRepository;
import com.zubair.taskpulse.repository.TaskRepository;
import com.zubair.taskpulse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderSchedulerService {

    private final TaskRepository taskRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    /**
     * Checks tasks for approaching deadlines (within 1 hour) and overdue tasks every 15 minutes.
     */
    @Scheduled(fixedRate = 900000) // 15 minutes in milliseconds
    @Transactional
    public void checkApproachingDeadlinesAndOverdueTasks() {
        log.info("Running scheduled reminder scan for approaching deadlines and overdue tasks...");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourLater = now.plusHours(1);

        List<Task> allTasks = taskRepository.findAll();

        for (Task task : allTasks) {
            if (task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.CANCELLED) {
                continue;
            }

            if (task.getDeadline() == null) {
                continue;
            }

            User user = task.getUser();
            if (user == null) {
                continue;
            }

            // Check approaching deadline (within 1 hour)
            if (task.getDeadline().isAfter(now) && task.getDeadline().isBefore(oneHourLater)) {
                boolean alreadyNotified = notificationRepository.existsByUserIdAndTaskIdAndTypeAndCreatedAtAfter(
                        user.getId(), task.getId(), NotificationType.DEADLINE_APPROACHING, now.minusHours(2)
                );

                if (!alreadyNotified) {
                    String title = "Deadline Approaching: " + task.getTitle();
                    String message = String.format("Task '%s' is due in less than an hour (at %s).",
                            task.getTitle(), task.getDeadline().toString().replace("T", " "));
                    notificationService.createNotification(user, task, title, message, NotificationType.DEADLINE_APPROACHING);
                }
            }

            // Check overdue task
            if (task.getDeadline().isBefore(now)) {
                boolean alreadyNotified = notificationRepository.existsByUserIdAndTaskIdAndTypeAndCreatedAtAfter(
                        user.getId(), task.getId(), NotificationType.TASK_OVERDUE, now.minusHours(12)
                );

                if (!alreadyNotified) {
                    String title = "Task Overdue: " + task.getTitle();
                    String message = String.format("Task '%s' was due at %s and is currently overdue.",
                            task.getTitle(), task.getDeadline().toString().replace("T", " "));
                    notificationService.createNotification(user, task, title, message, NotificationType.TASK_OVERDUE);
                }
            }
        }
    }

    /**
     * Generates daily summaries for all active users at 8:00 AM every morning.
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void generateDailySummaries() {
        log.info("Generating scheduled daily summaries for users...");
        List<User> users = userRepository.findAll();
        for (User user : users) {
            try {
                notificationService.generateDailySummary(user);
            } catch (Exception e) {
                log.error("Failed to generate daily summary for user: {}", user.getEmail(), e);
            }
        }
    }
}

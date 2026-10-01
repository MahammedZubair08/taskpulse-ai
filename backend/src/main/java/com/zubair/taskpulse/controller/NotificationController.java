package com.zubair.taskpulse.controller;

import com.zubair.taskpulse.dto.notification.NotificationResponse;
import com.zubair.taskpulse.entity.User;
import com.zubair.taskpulse.repository.UserRepository;
import com.zubair.taskpulse.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(Authentication authentication) {
        User user = getUser(authentication);
        List<NotificationResponse> notifications = notificationService.getNotificationsForUser(user);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        User user = getUser(authentication);
        long count = notificationService.getUnreadCount(user);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long id, Authentication authentication) {
        User user = getUser(authentication);
        NotificationResponse notification = notificationService.markAsRead(id, user);
        return ResponseEntity.ok(notification);
    }

    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllAsRead(Authentication authentication) {
        User user = getUser(authentication);
        int updated = notificationService.markAllAsRead(user);
        return ResponseEntity.ok(Map.of("updatedCount", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id, Authentication authentication) {
        User user = getUser(authentication);
        notificationService.deleteNotification(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/daily-summary")
    public ResponseEntity<NotificationResponse> generateDailySummary(Authentication authentication) {
        User user = getUser(authentication);
        NotificationResponse notification = notificationService.generateDailySummary(user);
        return ResponseEntity.ok(notification);
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + authentication.getName()));
    }
}

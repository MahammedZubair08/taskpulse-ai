package com.zubair.taskpulse.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zubair.taskpulse.dto.calendar.CalendarEventResponse;
import com.zubair.taskpulse.dto.scheduling.ApplyScheduleRequest;
import com.zubair.taskpulse.dto.scheduling.OptimizedScheduleResponse;
import com.zubair.taskpulse.dto.scheduling.ScheduledTaskBlock;
import com.zubair.taskpulse.entity.Task;
import com.zubair.taskpulse.entity.TaskPriority;
import com.zubair.taskpulse.entity.TaskStatus;
import com.zubair.taskpulse.entity.User;
import com.zubair.taskpulse.exception.AIProcessingException;
import com.zubair.taskpulse.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AISchedulingService {

    private final ChatClient chatClient;
    private final TaskRepository taskRepository;
    private final GoogleCalendarService googleCalendarService;
    private final ObjectMapper objectMapper;

    public OptimizedScheduleResponse generateOptimizedSchedule(User user, LocalDate targetDate) {
        if (targetDate == null) {
            targetDate = LocalDate.now();
        }

        List<Task> allTasks = taskRepository.findByUserId(user.getId());
        List<Task> pendingTasks = allTasks.stream()
                .filter(t -> t.getStatus() != TaskStatus.COMPLETED)
                .toList();

        if (pendingTasks.isEmpty()) {
            return new OptimizedScheduleResponse(targetDate, List.of(), "No active tasks found to schedule!");
        }

        List<CalendarEventResponse> calendarEvents = List.of();
        try {
            calendarEvents = googleCalendarService.fetchUpcomingEvents(
                    user,
                    targetDate.atStartOfDay(),
                    targetDate.atTime(23, 59, 59)
            );
        } catch (Exception e) {
            log.warn("Could not fetch calendar events for schedule optimization: {}", e.getMessage());
        }

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Target Date: ").append(targetDate).append("\n");
        promptBuilder.append("Working Hours: 08:00 AM to 09:00 PM\n\n");

        promptBuilder.append("User's Pending Tasks:\n");
        for (Task task : pendingTasks) {
            promptBuilder.append(String.format(
                    "- Task ID: %d | Title: \"%s\" | Priority: %s | Deadline: %s | Estimated Duration: %s minutes\n",
                    task.getId(),
                    task.getTitle(),
                    task.getPriority() != null ? task.getPriority().name() : "MEDIUM",
                    task.getDeadline() != null ? task.getDeadline().toString() : "None",
                    task.getEstimatedDurationMinutes() != null ? task.getEstimatedDurationMinutes() : 60
            ));
        }

        promptBuilder.append("\nExisting Google Calendar Commitments (DO NOT OVERLAP):\n");
        if (calendarEvents.isEmpty()) {
            promptBuilder.append("None (Calendar is clear)\n");
        } else {
            for (CalendarEventResponse evt : calendarEvents) {
                promptBuilder.append(String.format(
                        "- Event: \"%s\" | Start: %s | End: %s | AllDay: %b\n",
                        evt.summary(), evt.start(), evt.end(), evt.allDay()
                ));
            }
        }

        try {
            String aiRawResponse = callGeminiForSchedule(promptBuilder.toString());
            log.debug("Raw Gemini schedule response: {}", aiRawResponse);

            String cleanedJson = aiRawResponse.trim()
                    .replaceFirst("^```json\\s*", "")
                    .replaceFirst("^```\\s*", "")
                    .replaceFirst("\\s*```$", "")
                    .trim();

            JsonNode root = objectMapper.readTree(cleanedJson);
            String strategy = root.has("overallStrategy") ? root.get("overallStrategy").asText() : "AI Optimized Daily Schedule";

            List<ScheduledTaskBlock> blocks = new ArrayList<>();
            if (root.has("schedule") && root.get("schedule").isArray()) {
                for (JsonNode node : root.get("schedule")) {
                    Long taskId = node.has("taskId") ? node.get("taskId").asLong() : null;
                    String taskTitle = node.has("taskTitle") ? node.get("taskTitle").asText() : "";
                    String priorityStr = node.has("priority") ? node.get("priority").asText() : "MEDIUM";
                    TaskPriority priority = parsePriority(priorityStr);

                    LocalDateTime start = parseDateTime(node.get("startTime").asText());
                    LocalDateTime end = parseDateTime(node.get("endTime").asText());
                    Integer duration = node.has("durationMinutes") ? node.get("durationMinutes").asInt() : 60;
                    String reasoning = node.has("reasoning") ? node.get("reasoning").asText() : "";

                    blocks.add(new ScheduledTaskBlock(taskId, taskTitle, priority, start, end, duration, reasoning));
                }
            }

            return new OptimizedScheduleResponse(targetDate, blocks, strategy);

        } catch (Exception e) {
            log.error("Failed to generate AI schedule", e);
            throw new AIProcessingException("Failed to generate AI-optimized schedule. Please try again.", e);
        }
    }

    @Transactional
    public int applySchedule(User user, ApplyScheduleRequest request) {
        if (request.schedule() == null || request.schedule().isEmpty()) {
            return 0;
        }

        int count = 0;
        for (ScheduledTaskBlock block : request.schedule()) {
            if (block.taskId() == null) continue;

            Task task = taskRepository.findByIdAndUserId(block.taskId(), user.getId()).orElse(null);
            if (task == null) continue;

            task.setDeadline(block.endTime());
            if (block.durationMinutes() != null) {
                task.setEstimatedDurationMinutes(block.durationMinutes());
            }
            taskRepository.save(task);
            count++;

            if (request.exportToGoogleCalendar()) {
                try {
                    googleCalendarService.exportTaskToCalendar(user, task.getId(), block.startTime());
                } catch (Exception e) {
                    log.warn("Failed to export block for task ID {} to Google Calendar: {}", task.getId(), e.getMessage());
                }
            }
        }
        return count;
    }

    private String callGeminiForSchedule(String promptText) {
        return chatClient.prompt()
                .system("""
                You are TaskPulse AI's Master Productivity Scheduler.

                Your objective is to schedule pending tasks into an optimal daily time block schedule.

                Rules:
                1. Respect existing Google Calendar events — NEVER overlap a scheduled task with a calendar commitment.
                2. Prioritize URGENT and HIGH priority tasks earlier in the day and before their deadlines.
                3. Keep all scheduled start and end times within the target date (between 08:00 AM and 09:00 PM).
                4. Give each task an appropriate work window based on its estimated duration.
                5. Provide a brief, encouraging productivity reasoning for each task block.

                Return ONLY valid JSON matching this exact structure:
                {
                  "overallStrategy": "High-level summary of today's schedule optimization strategy",
                  "schedule": [
                    {
                      "taskId": 101,
                      "taskTitle": "Task title",
                      "priority": "LOW|MEDIUM|HIGH|URGENT",
                      "startTime": "YYYY-MM-DDTHH:mm:ss",
                      "endTime": "YYYY-MM-DDTHH:mm:ss",
                      "durationMinutes": 60,
                      "reasoning": "Reason for this time slot"
                    }
                  ]
                }
                """)
                .user(promptText)
                .call()
                .content();
    }

    private TaskPriority parsePriority(String priorityStr) {
        try {
            return TaskPriority.valueOf(priorityStr.toUpperCase());
        } catch (Exception e) {
            return TaskPriority.MEDIUM;
        }
    }

    private LocalDateTime parseDateTime(String text) {
        try {
            return LocalDateTime.parse(text.substring(0, 19));
        } catch (Exception e) {
            return LocalDateTime.now().plusHours(1);
        }
    }
}

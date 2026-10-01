package com.zubair.taskpulse.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zubair.taskpulse.dto.calendar.CalendarEventResponse;
import com.zubair.taskpulse.dto.calendar.ScheduleConflictResponse;
import com.zubair.taskpulse.dto.calendar.TimeSlotResponse;
import com.zubair.taskpulse.entity.Task;
import com.zubair.taskpulse.entity.TaskStatus;
import com.zubair.taskpulse.entity.User;
import com.zubair.taskpulse.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleCalendarService {

    private final GmailOAuthService gmailOAuthService;
    private final TaskRepository taskRepository;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public List<CalendarEventResponse> fetchUpcomingEvents(User user, LocalDateTime from, LocalDateTime to) {
        String accessToken = gmailOAuthService.getValidAccessToken(user);

        if (from == null) from = LocalDateTime.now();
        if (to == null) to = from.plusDays(14);

        String fromIso = from.atZone(ZoneId.systemDefault()).toInstant().toString();
        String toIso = to.atZone(ZoneId.systemDefault()).toInstant().toString();

        try {
            String url = "https://www.googleapis.com/calendar/v3/calendars/primary/events?" +
                    "timeMin=" + URLEncoder.encode(fromIso, StandardCharsets.UTF_8) +
                    "&timeMax=" + URLEncoder.encode(toIso, StandardCharsets.UTF_8) +
                    "&singleEvents=true&orderBy=startTime";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Failed to fetch Google Calendar events: {}", response.body());
                return List.of();
            }

            JsonNode root = objectMapper.readTree(response.body());
            List<CalendarEventResponse> events = new ArrayList<>();

            if (root.has("items") && root.get("items").isArray()) {
                for (JsonNode item : root.get("items")) {
                    String id = item.has("id") ? item.get("id").asText() : "";
                    String summary = item.has("summary") ? item.get("summary").asText() : "(No Title)";
                    String description = item.has("description") ? item.get("description").asText() : "";

                    boolean allDay = false;
                    LocalDateTime start = null;
                    LocalDateTime end = null;

                    if (item.has("start")) {
                        JsonNode startNode = item.get("start");
                        if (startNode.has("dateTime")) {
                            start = parseRfc3339(startNode.get("dateTime").asText());
                        } else if (startNode.has("date")) {
                            start = LocalDateTime.parse(startNode.get("date").asText() + "T00:00:00");
                            allDay = true;
                        }
                    }

                    if (item.has("end")) {
                        JsonNode endNode = item.get("end");
                        if (endNode.has("dateTime")) {
                            end = parseRfc3339(endNode.get("dateTime").asText());
                        } else if (endNode.has("date")) {
                            end = LocalDateTime.parse(endNode.get("date").asText() + "T23:59:59");
                        }
                    }

                    if (start != null && end != null) {
                        events.add(new CalendarEventResponse(id, summary, description, start, end, allDay));
                    }
                }
            }
            return events;

        } catch (Exception e) {
            log.error("Error fetching Google Calendar events for user {}", user.getEmail(), e);
            return List.of();
        }
    }

    public List<ScheduleConflictResponse> detectConflicts(User user) {
        List<Task> tasks = taskRepository.findByUserId(user.getId());
        List<CalendarEventResponse> events = fetchUpcomingEvents(user, LocalDateTime.now(), LocalDateTime.now().plusDays(14));

        List<ScheduleConflictResponse> conflicts = new ArrayList<>();

        for (Task task : tasks) {
            if (task.getStatus() == TaskStatus.COMPLETED || task.getDeadline() == null) {
                continue;
            }

            int durationMinutes = task.getEstimatedDurationMinutes() != null ? task.getEstimatedDurationMinutes() : 60;
            LocalDateTime taskStart = task.getDeadline().minusMinutes(durationMinutes);
            LocalDateTime taskEnd = task.getDeadline();

            for (CalendarEventResponse event : events) {
                if (event.allDay()) continue;

                // Overlap condition: taskStart < eventEnd AND taskEnd > eventStart
                if (taskStart.isBefore(event.end()) && taskEnd.isAfter(event.start())) {
                    conflicts.add(new ScheduleConflictResponse(
                            task.getId(),
                            task.getTitle(),
                            task.getDeadline(),
                            event.summary(),
                            event.start(),
                            event.end()
                    ));
                }
            }
        }
        return conflicts;
    }

    public List<TimeSlotResponse> findFreeTimeSlots(User user, Integer durationMinutes, LocalDateTime deadline) {
        if (durationMinutes == null || durationMinutes <= 0) durationMinutes = 60;
        if (deadline == null) deadline = LocalDateTime.now().plusDays(7);

        LocalDateTime now = LocalDateTime.now();
        List<CalendarEventResponse> events = fetchUpcomingEvents(user, now, deadline);

        List<TimeSlotResponse> freeSlots = new ArrayList<>();
        LocalDateTime candidateStart = now.plusMinutes(15).withSecond(0).withNano(0);

        while (candidateStart.isBefore(deadline) && freeSlots.size() < 5) {
            // Constrain working hours (8:00 AM to 9:00 PM)
            if (candidateStart.getHour() < 8) {
                candidateStart = candidateStart.withHour(8).withMinute(0);
            } else if (candidateStart.getHour() >= 21) {
                candidateStart = candidateStart.plusDays(1).withHour(8).withMinute(0);
                continue;
            }

            LocalDateTime candidateEnd = candidateStart.plusMinutes(durationMinutes);
            if (candidateEnd.isAfter(deadline)) break;

            final LocalDateTime checkStart = candidateStart;
            final LocalDateTime checkEnd = candidateEnd;

            boolean overlaps = events.stream().anyMatch(e ->
                    !e.allDay() && checkStart.isBefore(e.end()) && checkEnd.isAfter(e.start())
            );

            if (!overlaps) {
                freeSlots.add(new TimeSlotResponse(candidateStart, candidateEnd, durationMinutes));
                candidateStart = candidateStart.plusMinutes(Math.max(30, durationMinutes));
            } else {
                candidateStart = candidateStart.plusMinutes(30);
            }
        }

        return freeSlots;
    }

    public CalendarEventResponse exportTaskToCalendar(User user, Long taskId, LocalDateTime startTime) {
        Task task = taskRepository.findByIdAndUserId(taskId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + taskId));

        String accessToken = gmailOAuthService.getValidAccessToken(user);

        if (startTime == null) {
            startTime = task.getDeadline() != null ? task.getDeadline().minusHours(1) : LocalDateTime.now().plusHours(1);
        }

        int duration = task.getEstimatedDurationMinutes() != null ? task.getEstimatedDurationMinutes() : 60;
        LocalDateTime endTime = startTime.plusMinutes(duration);

        String startIso = startTime.atZone(ZoneId.systemDefault()).toInstant().toString();
        String endIso = endTime.atZone(ZoneId.systemDefault()).toInstant().toString();

        try {
            com.fasterxml.jackson.databind.node.ObjectNode rootNode = objectMapper.createObjectNode();
            rootNode.put("summary", "TaskPulse: " + task.getTitle());
            rootNode.put("description", task.getDescription() != null ? task.getDescription() : "Task from TaskPulse AI");
            rootNode.set("start", objectMapper.createObjectNode().put("dateTime", startIso));
            rootNode.set("end", objectMapper.createObjectNode().put("dateTime", endIso));

            String jsonBody = objectMapper.writeValueAsString(rootNode);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.googleapis.com/calendar/v3/calendars/primary/events"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 201) {
                log.error("Failed to export task to Google Calendar: {}", response.body());
                throw new RuntimeException("Google Calendar export failed: " + response.body());
            }

            JsonNode node = objectMapper.readTree(response.body());
            String eventId = node.has("id") ? node.get("id").asText() : "";
            String summary = node.has("summary") ? node.get("summary").asText() : task.getTitle();

            log.info("Exported task '{}' to Google Calendar event ID {}", task.getTitle(), eventId);
            return new CalendarEventResponse(eventId, summary, task.getDescription(), startTime, endTime, false);

        } catch (Exception e) {
            log.error("Error exporting task to Google Calendar", e);
            throw new RuntimeException("Failed to export task to Google Calendar", e);
        }
    }

    private LocalDateTime parseRfc3339(String dateStr) {
        try {
            Instant instant = Instant.parse(dateStr);
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dateStr.substring(0, 19));
            } catch (Exception ex) {
                return LocalDateTime.now();
            }
        }
    }
}

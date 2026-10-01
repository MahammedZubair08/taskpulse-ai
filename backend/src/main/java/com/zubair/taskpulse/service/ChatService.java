package com.zubair.taskpulse.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zubair.taskpulse.dto.chat.ChatIntent;
import com.zubair.taskpulse.dto.chat.ChatRequest;
import com.zubair.taskpulse.dto.chat.ChatResponse;
import com.zubair.taskpulse.dto.task.TaskResponse;
import com.zubair.taskpulse.entity.TaskPriority;
import com.zubair.taskpulse.entity.TaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final TaskService taskService;
    public ChatResponse processMessage(
            ChatRequest request,
            String userEmail
    ) {
        String message = request.message().trim();
        if (message.isBlank()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }
        ChatIntent intent = extractIntent(message);
        List<TaskResponse> userTasks = taskService.getTasks(
                userEmail,
                null,
                null
        );
        List<TaskResponse> matchingTasks = filterTasks(
                userTasks,
                intent
        );
        String responseMessage = buildResponse(
                intent,
                matchingTasks
        );
        return new ChatResponse(
                responseMessage,
                matchingTasks
        );
    }
    private ChatIntent extractIntent(String message) {
        String response = chatClient.prompt()
                .system("""
                        You are TaskPulse's task assistant.
                        Your ONLY job is to classify the user's message
                        into one of the supported task-query intents.
                        Supported intents:
                        ALL_TASKS
                        - User asks to see all tasks, tasks in general,
                          my tasks, or what tasks they have.
                        DUE_TODAY
                        - User asks what is due today.
                        DUE_THIS_WEEK
                        - User asks what is due this week.
                        OVERDUE
                        - User asks about overdue or late tasks.
                        BY_PRIORITY
                        - User asks for tasks of a particular priority.
                        Supported priorities:
                        LOW, MEDIUM, HIGH, URGENT
                        BY_STATUS
                        - User asks for tasks with a particular status.
                        Supported statuses:
                        TODO, IN_PROGRESS, COMPLETED
                        SEARCH
                        - User asks to find/search tasks matching a word
                          or phrase in the task title.
                        UNKNOWN
                        - The message is not a supported task query.
                        Rules:
                        - Do not invent information.
                        - Do not execute any task changes.
                        - Do not create, update, delete, or complete tasks.
                        - Extract only the intent explicitly expressed.
                        - For SEARCH, put the search phrase in query.
                        - For BY_PRIORITY, put the priority in priority.
                        - For BY_STATUS, put the status in status.
                        - For other intents, query, priority and status should be null.
                        Return ONLY valid JSON.
                        Exact structure:
                        {
                          "intent": "ALL_TASKS|DUE_TODAY|DUE_THIS_WEEK|OVERDUE|BY_PRIORITY|BY_STATUS|SEARCH|UNKNOWN",
                          "query": "search phrase or null",
                          "priority": "LOW|MEDIUM|HIGH|URGENT or null",
                          "status": "TODO|IN_PROGRESS|COMPLETED or null"
                        }
                        """)
                .user(message)
                .call()
                .content();
        try {
            response = cleanJson(response);
            ChatIntent intent =
                    objectMapper.readValue(
                            response,
                            ChatIntent.class
                    );
            if (intent.intent() == null) {
                return new ChatIntent(
                        ChatIntent.IntentType.UNKNOWN,
                        null,
                        null,
                        null
                );
            }
            return intent;
        } catch (Exception e) {
            log.error(
                    "Failed to parse chat intent. Gemini response: {}",
                    response,
                    e
            );
            return new ChatIntent(
                    ChatIntent.IntentType.UNKNOWN,
                    null,
                    null,
                    null
            );
        }
    }
    private List<TaskResponse> filterTasks(
            List<TaskResponse> tasks,
            ChatIntent intent
    ) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday =
                today.atStartOfDay();
        LocalDateTime startOfTomorrow =
                today.plusDays(1).atStartOfDay();
        LocalDate startOfWeek =
                today.with(
                        TemporalAdjusters.previousOrSame(
                                DayOfWeek.MONDAY
                        )
                );
        LocalDate endOfWeek =
                startOfWeek.plusDays(6);
        LocalDateTime startOfNextWeek =
                endOfWeek.plusDays(1).atStartOfDay();
        return tasks.stream()
                .filter(task -> matchesIntent(
                        task,
                        intent,
                        startOfToday,
                        startOfTomorrow,
                        startOfWeek.atStartOfDay(),
                        startOfNextWeek
                ))
                .toList();
    }
    private boolean matchesIntent(
            TaskResponse task,
            ChatIntent intent,
            LocalDateTime startOfToday,
            LocalDateTime startOfTomorrow,
            LocalDateTime startOfWeek,
            LocalDateTime startOfNextWeek
    ) {
        if (intent.intent() == null) {
            return false;
        }
        return switch (intent.intent()) {
            case ALL_TASKS ->
                    true;
            case DUE_TODAY ->
                    task.getDeadline() != null
                            && !task.getDeadline().isBefore(startOfToday)
                            && task.getDeadline().isBefore(startOfTomorrow);
            case DUE_THIS_WEEK ->
                    task.getDeadline() != null
                            && !task.getDeadline().isBefore(startOfWeek)
                            && task.getDeadline().isBefore(startOfNextWeek);
            case OVERDUE ->
                    task.getDeadline() != null
                            && task.getDeadline().isBefore(LocalDateTime.now())
                            && task.getStatus() != TaskStatus.COMPLETED;
            case BY_PRIORITY ->
                    matchesPriority(task, intent.priority());
            case BY_STATUS ->
                    matchesStatus(task, intent.status());
            case SEARCH ->
                    matchesSearch(task, intent.query());
            case UNKNOWN ->
                    false;
        };
    }
    private boolean matchesPriority(
            TaskResponse task,
            String priority
    ) {
        if (task.getPriority() == null || priority == null) {
            return false;
        }
        try {
            return task.getPriority() ==
                    TaskPriority.valueOf(
                            priority.toUpperCase(Locale.ROOT)
                    );
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    private boolean matchesStatus(
            TaskResponse task,
            String status
    ) {
        if (task.getStatus() == null || status == null) {
            return false;
        }
        try {
            return task.getStatus() ==
                    TaskStatus.valueOf(
                            status.toUpperCase(Locale.ROOT)
                    );
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    private boolean matchesSearch(
            TaskResponse task,
            String query
    ) {
        if (task.getTitle() == null || query == null) {
            return false;
        }
        return task.getTitle()
                .toLowerCase(Locale.ROOT)
                .contains(
                        query.trim()
                                .toLowerCase(Locale.ROOT)
                );
    }
    private String buildResponse(
            ChatIntent intent,
            List<TaskResponse> tasks
    ) {
        if (intent.intent() == ChatIntent.IntentType.UNKNOWN) {
            return """
                    I can help you find and view your tasks.
                    Try:
                    • Show my tasks
                    • What is due today?
                    • What is due this week?
                    • Show overdue tasks
                    • Show high priority tasks
                    • Show completed tasks
                    • Find my assignment tasks
                    """;
        }
        if (tasks.isEmpty()) {
            return switch (intent.intent()) {
                case ALL_TASKS ->
                        "You currently have no tasks.";
                case DUE_TODAY ->
                        "You have no tasks due today.";
                case DUE_THIS_WEEK ->
                        "You have no tasks due this week.";
                case OVERDUE ->
                        "You have no overdue tasks.";
                case BY_PRIORITY ->
                        "You have no tasks matching that priority.";
                case BY_STATUS ->
                        "You have no tasks matching that status.";
                case SEARCH ->
                        "I couldn't find any tasks matching your search.";
                case UNKNOWN ->
                        "I couldn't understand that request.";
            };
        }
        return switch (intent.intent()) {
            case ALL_TASKS ->
                    "You have " + tasks.size() + " task(s).";
            case DUE_TODAY ->
                    "You have " + tasks.size()
                            + " task(s) due today.";
            case DUE_THIS_WEEK ->
                    "You have " + tasks.size()
                            + " task(s) due this week.";
            case OVERDUE ->
                    "You have " + tasks.size()
                            + " overdue task(s).";
            case BY_PRIORITY ->
                    "I found " + tasks.size()
                            + " task(s) with that priority.";
            case BY_STATUS ->
                    "I found " + tasks.size()
                            + " task(s) with that status.";
            case SEARCH ->
                    "I found " + tasks.size()
                            + " matching task(s).";
            case UNKNOWN ->
                    "I couldn't understand that request.";
        };
    }
    private String cleanJson(String response) {
        if (response == null) {
            return "";
        }
        return response
                .trim()
                .replaceFirst("^```json\\s*", "")
                .replaceFirst("^```\\s*", "")
                .replaceFirst("\\s*```$", "")
                .trim();
    }
}

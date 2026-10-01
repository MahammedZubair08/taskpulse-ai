package com.zubair.taskpulse.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zubair.taskpulse.dto.ai.AITaskResponse;
import com.zubair.taskpulse.dto.gmail.GmailSuggestionResponse;
import com.zubair.taskpulse.dto.gmail.GmailSyncResultResponse;
import com.zubair.taskpulse.dto.task.CreateTaskRequest;
import com.zubair.taskpulse.dto.task.TaskResponse;
import com.zubair.taskpulse.entity.TaskPriority;
import com.zubair.taskpulse.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GmailService {

    private final GmailOAuthService gmailOAuthService;
    private final AIService aiService;
    private final TaskService taskService;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public GmailSyncResultResponse fetchTaskSuggestions(User user) {
        String accessToken = gmailOAuthService.getValidAccessToken(user);

        List<String> messageIds = fetchRecentUnreadMessageIds(accessToken, 10);
        log.info("Found {} unread emails for user {}", messageIds.size(), user.getEmail());

        List<TaskResponse> autoCreatedTasks = new ArrayList<>();
        List<GmailSuggestionResponse> suggestions = new ArrayList<>();

        for (String messageId : messageIds) {
            try {
                EmailMetadata email = fetchEmailDetails(accessToken, messageId);
                if (email == null) continue;

                // Prepare prompt for AI extraction from email
                String promptText = String.format(
                        "Email Subject: %s\nFrom: %s\nSnippet: %s\n\nContent:\n%s",
                        email.subject, email.from, email.snippet, email.body
                );

                AITaskResponse aiResponse = aiService.extractTask(promptText);

                if (aiResponse != null && aiResponse.title() != null && !aiResponse.title().isBlank()) {
                    TaskPriority priority = aiResponse.priority() != null ? aiResponse.priority() : TaskPriority.MEDIUM;
                    LocalDateTime deadline = aiResponse.deadline();

                    // If deadline is past, reset to null to pass future validation
                    if (deadline != null && deadline.isBefore(LocalDateTime.now())) {
                        deadline = null;
                    }

                    // Rule: Auto-create HIGH and URGENT priority tasks; treat LOW and MEDIUM as suggestions
                    if (priority == TaskPriority.HIGH || priority == TaskPriority.URGENT) {
                        CreateTaskRequest createTaskRequest = new CreateTaskRequest();
                        createTaskRequest.setTitle(aiResponse.title());
                        createTaskRequest.setDescription(String.format("⚡ Auto-created from email: \"%s\" from %s", email.subject, email.from));
                        createTaskRequest.setPriority(priority);
                        createTaskRequest.setDeadline(deadline);
                        createTaskRequest.setEstimatedDurationMinutes(aiResponse.estimatedDurationMinutes());

                        TaskResponse createdTask = taskService.createTask(createTaskRequest, user.getEmail());
                        autoCreatedTasks.add(createdTask);
                        log.info("Auto-created HIGH/URGENT task from email '{}' for user {}", email.subject, user.getEmail());
                    } else {
                        suggestions.add(new GmailSuggestionResponse(
                                email.id,
                                email.subject,
                                email.from,
                                email.snippet,
                                aiResponse.title(),
                                priority,
                                aiResponse.deadlineExpression(),
                                deadline,
                                aiResponse.estimatedDurationMinutes()
                        ));
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to process email message ID {}: {}", messageId, e.getMessage());
            }
        }

        return new GmailSyncResultResponse(autoCreatedTasks, suggestions);
    }

    private List<String> fetchRecentUnreadMessageIds(String accessToken, int limit) {
        try {
            String url = "https://gmail.googleapis.com/gmail/v1/users/me/messages?maxResults=" + limit + "&q=is:unread";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Failed to list Gmail messages: {}", response.body());
                return List.of();
            }

            JsonNode root = objectMapper.readTree(response.body());
            List<String> ids = new ArrayList<>();
            if (root.has("messages") && root.get("messages").isArray()) {
                for (JsonNode msgNode : root.get("messages")) {
                    ids.add(msgNode.get("id").asText());
                }
            }
            return ids;
        } catch (Exception e) {
            log.error("Error fetching message list from Gmail API", e);
            return List.of();
        }
    }

    private EmailMetadata fetchEmailDetails(String accessToken, String messageId) {
        try {
            String url = "https://gmail.googleapis.com/gmail/v1/users/me/messages/" + messageId + "?format=full";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return null;
            }

            JsonNode node = objectMapper.readTree(response.body());
            String id = node.get("id").asText();
            String snippet = node.has("snippet") ? node.get("snippet").asText() : "";

            String subject = "(No Subject)";
            String from = "(Unknown)";

            JsonNode payload = node.get("payload");
            if (payload != null && payload.has("headers")) {
                for (JsonNode header : payload.get("headers")) {
                    String name = header.get("name").asText();
                    if ("Subject".equalsIgnoreCase(name)) {
                        subject = header.get("value").asText();
                    } else if ("From".equalsIgnoreCase(name)) {
                        from = header.get("value").asText();
                    }
                }
            }

            String bodyText = extractBodyFromPayload(payload);
            if (bodyText == null || bodyText.isBlank()) {
                bodyText = snippet;
            }

            return new EmailMetadata(id, subject, from, snippet, bodyText);

        } catch (Exception e) {
            log.error("Error fetching email detail for id {}", messageId, e);
            return null;
        }
    }

    private String extractBodyFromPayload(JsonNode payload) {
        if (payload == null) return "";

        if (payload.has("body") && payload.get("body").has("data")) {
            return decodeBase64Url(payload.get("body").get("data").asText());
        }

        if (payload.has("parts") && payload.get("parts").isArray()) {
            for (JsonNode part : payload.get("parts")) {
                String mimeType = part.has("mimeType") ? part.get("mimeType").asText() : "";
                if ("text/plain".equalsIgnoreCase(mimeType) && part.has("body") && part.get("body").has("data")) {
                    return decodeBase64Url(part.get("body").get("data").asText());
                }
            }
            for (JsonNode part : payload.get("parts")) {
                if (part.has("body") && part.get("body").has("data")) {
                    return decodeBase64Url(part.get("body").get("data").asText());
                }
            }
        }
        return "";
    }

    private String decodeBase64Url(String base64UrlStr) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(base64UrlStr);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private record EmailMetadata(
            String id,
            String subject,
            String from,
            String snippet,
            String body
    ) {}
}

package com.zubair.taskpulse.service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zubair.taskpulse.dto.ai.AITaskResponse;
import com.zubair.taskpulse.dto.task.CreateTaskRequest;
import com.zubair.taskpulse.entity.TelegramConnection;
import com.zubair.taskpulse.entity.TelegramSuggestion;
import com.zubair.taskpulse.repository.TelegramConnectionRepository;
import com.zubair.taskpulse.repository.TelegramSuggestionRepository;
import com.zubair.taskpulse.service.AIService;
import com.zubair.taskpulse.service.TaskService;
import com.zubair.taskpulse.service.DeadlineParser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.time.LocalDateTime;
import java.util.Optional;
@Service
@RequiredArgsConstructor
public class TelegramService {
    private final TelegramConnectionRepository connectionRepository;
    private final TelegramSuggestionRepository suggestionRepository;
    private final AIService aiService;
    private final TaskService taskService;
    private final DeadlineParser deadlineParser;
    private final ObjectMapper objectMapper;
    @Value("${telegram.bot.token}")
    private String botToken;
    private final RestClient restClient = RestClient.builder().build();
    public void handleUpdate(String updateJson) {
        try {
            JsonNode root = objectMapper.readTree(updateJson);
            JsonNode message = root.get("message");
            if (message == null || message.isNull()) {
                return;
            }
            JsonNode chat = message.get("chat");
            JsonNode from = message.get("from");
            JsonNode textNode = message.get("text");
            if (chat == null || textNode == null) {
                return;
            }
            Long chatId = chat.get("id").asLong();
            String text = textNode.asText().trim();
            String username = null;
            if (from != null && from.has("username")) {
                username = from.get("username").asText();
            }
            processMessage(chatId, username, text);
        } catch (Exception e) {
            System.err.println("Telegram update processing failed: "
                    + e.getMessage());
        }
    }
    private void processMessage(
            Long chatId,
            String username,
            String text
    ) {
        if (text.equalsIgnoreCase("/start")) {
            sendMessage(
                    chatId,
                    """
                    ?? Welcome to TaskPulse AI!
                    Connect your TaskPulse account using:
                    /connect YOUR_CODE
                    After connecting, you can send messages such as:
                    "Submit DBMS assignment by Friday, urgent"
                    I'll convert it into a task suggestion for you.
                    """
            );
            return;
        }
        if (text.toLowerCase().startsWith("/connect ")) {
            handleConnect(chatId, username, text.substring(9).trim());
            return;
        }
        Optional<TelegramConnection> connection =
                connectionRepository.findByTelegramChatId(chatId);
        if (connection.isEmpty()) {
            sendMessage(
                    chatId,
                    "Your Telegram account is not connected to TaskPulse.\n\n"
                            + "Use /connect YOUR_CODE"
            );
            return;
        }
        if (text.equalsIgnoreCase("/confirm")) {
            confirmLatestSuggestion(chatId);
            return;
        }
        if (text.equalsIgnoreCase("/dismiss")) {
            dismissLatestSuggestion(chatId);
            return;
        }
        if (text.equalsIgnoreCase("/help")) {
            sendMessage(
                    chatId,
                    """
                    TaskPulse Telegram commands:
                    /confirm - create the latest suggested task
                    /dismiss - dismiss the latest suggestion
                    /help - show this help
                    Or simply send a task description.
                    """
            );
            return;
        }
        createSuggestion(
                chatId,
                connection.get().getUserEmail(),
                text
        );
    }
    private void handleConnect(
            Long chatId,
            String username,
            String code
    ) {
        // Connection-code validation will be connected to
        // the TaskPulse web application in the next step.
        //
        // For now this intentionally does not blindly connect
        // an arbitrary email/account.
        sendMessage(
                chatId,
                "Connection codes are not configured yet. "
                        + "Generate the code from your TaskPulse account."
        );
    }
    private void createSuggestion(
            Long chatId,
            String userEmail,
            String message
    ) {
        try {
            AITaskResponse aiTask = aiService.extractTask(message);
            LocalDateTime deadline =
                    deadlineParser.parse(aiTask.deadlineExpression());
            TelegramSuggestion suggestion =
                    TelegramSuggestion.builder()
                            .telegramChatId(chatId)
                            .userEmail(userEmail)
                            .originalMessage(message)
                            .title(aiTask.title())
                            .priority(
                                    aiTask.priority() != null
                                            ? aiTask.priority().toString()
                                            : null
                            )
                            .deadlineExpression(
                                    aiTask.deadlineExpression()
                            )
                            .deadline(deadline)
                            .estimatedDuration(
                                    aiTask.estimatedDurationMinutes()
                            )
                            .status(TelegramSuggestion.Status.PENDING)
                            .build();
            suggestionRepository.save(suggestion);
            StringBuilder response = new StringBuilder();
            response.append("?? Task suggestion\n\n");
            response.append("Title: ")
                    .append(aiTask.title())
                    .append("\n");
            if (aiTask.priority() != null) {
                response.append("Priority: ")
                        .append(aiTask.priority())
                        .append("\n");
            }
            if (aiTask.deadlineExpression() != null) {
                response.append("Deadline: ")
                        .append(aiTask.deadlineExpression())
                        .append("\n");
            }
            if (aiTask.estimatedDurationMinutes() != null) {
                response.append("Duration: ")
                        .append(aiTask.estimatedDurationMinutes())
                        .append(" minutes\n");
            }
            response.append("\n");
            response.append("Reply /confirm to create this task.");
            response.append("\nReply /dismiss to discard it.");
            sendMessage(chatId, response.toString());
        } catch (Exception e) {
            sendMessage(
                    chatId,
                    "I couldn't convert that message into a task.\n\n"
                            + "Please try describing the task more clearly."
            );
            System.err.println(
                    "Telegram AI extraction failed: "
                            + e.getMessage()
            );
        }
    }
    private void confirmLatestSuggestion(Long chatId) {
        Optional<TelegramSuggestion> optional =
                suggestionRepository
                        .findFirstByTelegramChatIdAndStatusOrderByCreatedAtDesc(
                                chatId,
                                TelegramSuggestion.Status.PENDING
                        );
        if (optional.isEmpty()) {
            sendMessage(
                    chatId,
                    "There is no pending task suggestion to confirm."
            );
            return;
        }
        TelegramSuggestion suggestion = optional.get();
        try {
            CreateTaskRequest request =
                    new CreateTaskRequest(
                            suggestion.getTitle(),
                            "",
                            suggestion.getPriority(),
                            suggestion.getDeadline(),
                            suggestion.getEstimatedDurationM()
                    );
            taskService.createTask(
                    request,
                    suggestion.getUserEmail()
            );
            suggestion.setStatus(
                    TelegramSuggestion.Status.CONFIRMED
            );
            suggestionRepository.save(suggestion);
            sendMessage(
                    chatId,
                    "? Task created successfully!\n\n"
                            + suggestion.getTitle()
            );
        } catch (Exception e) {
            sendMessage(
                    chatId,
                    "? I couldn't create the task.\n\n"
                            + e.getMessage()
            );
        }
    }
    private void dismissLatestSuggestion(Long chatId) {
        Optional<TelegramSuggestion> optional =
                suggestionRepository
                        .findFirstByTelegramChatIdAndStatusOrderByCreatedAtDesc(
                                chatId,
                                TelegramSuggestion.Status.PENDING
                        );
        if (optional.isEmpty()) {
            sendMessage(
                    chatId,
                    "There is no pending suggestion to dismiss."
            );
            return;
        }
        TelegramSuggestion suggestion = optional.get();
        suggestion.setStatus(
                TelegramSuggestion.Status.DISMISSED
        );
        suggestionRepository.save(suggestion);
        sendMessage(
                chatId,
                "??? Task suggestion dismissed."
        );
    }
    private void sendMessage(Long chatId, String text) {
        String url =
                "https://api.telegram.org/bot"
                        + botToken
                        + "/sendMessage";
        restClient.post()
                .uri(url)
                .body(
                        java.util.Map.of(
                                "chat_id", chatId,
                                "text", text
                        )
                )
                .retrieve()
                .toBodilessEntity();
    }
}

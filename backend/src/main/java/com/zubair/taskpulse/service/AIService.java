package com.zubair.taskpulse.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.zubair.taskpulse.dto.ai.AITaskResponse;
import com.zubair.taskpulse.exception.AIProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final AIMetricsService aiMetricsService;
    private void validateTask(AITaskResponse task) {

        if (task.title() == null || task.title().isBlank()) {
            throw new IllegalArgumentException(
                    "AI generated an empty task title"
            );
        }

        if (task.title().length() > 200) {
            throw new IllegalArgumentException(
                    "AI generated title exceeds 200 characters"
            );
        }

        if (task.priority() == null) {
            throw new IllegalArgumentException(
                    "AI did not provide a valid priority"
            );
        }

        if (task.estimatedDurationMinutes() != null) {

            if (task.estimatedDurationMinutes() < 1 ||
                    task.estimatedDurationMinutes() > 1440) {

                throw new IllegalArgumentException(
                        "AI generated invalid estimated duration"
                );
            }
        }
    }
    @Cacheable(
            value = "ai-task-extraction",
            key = "#userPrompt + '|' + T(java.time.LocalDate).now()"
    )
    public AITaskResponse extractTask(String userPrompt) {

        String response = callGeminiWithRetry(userPrompt);

        try {

            // Remove Markdown code fences if Gemini adds them
            response = response
                    .trim()
                    .replaceFirst("^```json\\s*", "")
                    .replaceFirst("^```\\s*", "")
                    .replaceFirst("\\s*```$", "")
                    .trim();

            log.debug("Cleaned Gemini response: {}", response);

            AITaskResponse task =
                    objectMapper.readValue(
                            response,
                            AITaskResponse.class
                    );

            validateTask(task);

            return task;

        } catch (Exception e) {

            log.error(
                    "Failed to parse Gemini response: {}",
                    response,
                    e
            );

            throw new AIProcessingException(
                    "Gemini returned an invalid task response.",
                    e
            );
        }
    }
    private String callGemini(String userPrompt) {

        return chatClient.prompt()
                .system("""
                You are TaskPulse's task extraction model.

                Extract ONLY information explicitly stated or directly
                implied by the user.

                Rules:
                - Do not invent dates or times.
                - Do not invent priorities.
                - Do not invent durations.
                - Preserve the user's deadline expression.
                - Convert explicit durations into minutes.
                - If no duration is stated, return null.
                - If no deadline is stated, return null.
                - If no priority is stated, use MEDIUM.
                - "urgent" means URGENT.
                - "high priority" means HIGH.
                - "low priority" means LOW.
                - The title must be short and actionable.

                IMPORTANT:
                - Return ONLY the JSON object.
                - Do NOT use Markdown.
                - Do NOT use ```json.
                - Do NOT include explanations.
                - Do NOT include text before or after the JSON.

                Return exactly this structure:

                {
                  "title": "short actionable task title",
                  "priority": "LOW|MEDIUM|HIGH|URGENT",
                  "deadlineExpression": "exact deadline expression from the user or null",
                  "estimatedDurationMinutes": number or null
                }
                """)
                .user(userPrompt)
                .call()
                .content();
    }
    private String callGeminiWithRetry(String userPrompt) {

        int maxAttempts = 3;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            try {

                return callGemini(userPrompt);

            } catch (Exception e) {

                log.warn(
                        "Gemini request failed. Attempt {}/{}",
                        attempt,
                        maxAttempts,
                        e
                );

                if (attempt == maxAttempts) {
                    throw new AIProcessingException(
                            "AI service is temporarily unavailable.",
                            e
                    );
                }

                try {
                    Thread.sleep(1000L * attempt);
                } catch (InterruptedException interruptedException) {

                    Thread.currentThread().interrupt();

                    throw new AIProcessingException(
                            "AI request was interrupted.",
                            interruptedException
                    );
                }
            }
        }

        throw new AIProcessingException(
                "AI service unavailable."
        );
    }
}
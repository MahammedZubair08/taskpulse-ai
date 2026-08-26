package com.zubair.taskpulse.controller;

import com.zubair.taskpulse.dto.ai.AITaskRequest;
import com.zubair.taskpulse.dto.ai.AITaskResponse;
import com.zubair.taskpulse.dto.task.CreateTaskRequest;
import com.zubair.taskpulse.dto.task.TaskResponse;
import com.zubair.taskpulse.service.AIRateLimitService;
import com.zubair.taskpulse.service.AIService;
import com.zubair.taskpulse.service.DeadlineParser;
import com.zubair.taskpulse.service.TaskService;

import com.zubair.taskpulse.service.impl.TaskServiceImpl;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {
    private final TaskServiceImpl taskService;
    private final AIService aiService;
    private final DeadlineParser deadlineParser;
    private final AIRateLimitService aiRateLimitService;
    @PostMapping("/extract-task")
    public Map<String, String> extractTask(
            @RequestBody Map<String, String> request
    ) {

        String prompt = request.get("prompt");

        String result = String.valueOf(aiService.extractTask(prompt));

        return Map.of("result", result);
    }
    @PostMapping("/tasks")
    public ResponseEntity<TaskResponse> createTaskFromAI(
            @Valid @RequestBody AITaskRequest request,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();

        if (!aiRateLimitService.isAllowed(userEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "AI request limit exceeded. Try again later."
            );
        }
        AITaskResponse aiTask =
                aiService.extractTask(request.prompt());

        CreateTaskRequest taskRequest = new CreateTaskRequest();

        taskRequest.setTitle(aiTask.title());
        taskRequest.setDescription(null);
        taskRequest.setPriority(aiTask.priority());
        taskRequest.setEstimatedDurationMinutes(
                aiTask.estimatedDurationMinutes()
        );

        taskRequest.setDeadline(
                deadlineParser.parse(aiTask.deadlineExpression())
        );

        TaskResponse response =
                taskService.createTask(
                        taskRequest,
                        authentication.getName()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
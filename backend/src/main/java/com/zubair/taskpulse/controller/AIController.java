package com.zubair.taskpulse.controller;

import com.zubair.taskpulse.dto.ai.AIConfirmTaskRequest;
import com.zubair.taskpulse.dto.ai.AITaskRequest;
import com.zubair.taskpulse.dto.ai.AITaskResponse;
import com.zubair.taskpulse.dto.task.CreateTaskRequest;
import com.zubair.taskpulse.dto.task.TaskResponse;
import com.zubair.taskpulse.service.AIRateLimitService;
import com.zubair.taskpulse.service.AIService;
import com.zubair.taskpulse.service.DeadlineParser;
import com.zubair.taskpulse.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {
        private final TaskService taskService;
        private final AIService aiService;
        private final DeadlineParser deadlineParser;
        private final AIRateLimitService aiRateLimitService;

        @PostMapping("/extract-task")
        public ResponseEntity<AITaskResponse> extractTask(
                        @Valid @RequestBody AITaskRequest request) {

                AITaskResponse aiTask = aiService.extractTask(request.prompt());

                LocalDateTime deadline = deadlineParser.parse(
                                aiTask.deadlineExpression());

                AITaskResponse response = new AITaskResponse(
                                aiTask.title(),
                                aiTask.priority(),
                                aiTask.deadlineExpression(),
                                deadline,
                                aiTask.estimatedDurationMinutes());

                return ResponseEntity.ok(response);
        }

        @PostMapping("/tasks")
        public ResponseEntity<TaskResponse> createTaskFromAI(
                        @Valid @RequestBody AIConfirmTaskRequest request,
                        Authentication authentication) {

                String userEmail = authentication.getName();

                CreateTaskRequest taskRequest = new CreateTaskRequest();

                taskRequest.setTitle(
                                request.title());

                taskRequest.setDescription(
                                request.description());

                taskRequest.setPriority(
                                request.priority());

                taskRequest.setDeadline(
                                request.deadline());

                taskRequest.setEstimatedDurationMinutes(
                                request.estimatedDurationMinutes());

                TaskResponse response = taskService.createTask(
                                taskRequest,
                                userEmail);

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(response);
        }
}

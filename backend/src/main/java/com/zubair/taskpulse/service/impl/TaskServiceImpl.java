package com.zubair.taskpulse.service.impl;

import com.zubair.taskpulse.dto.task.CreateTaskRequest;
import com.zubair.taskpulse.dto.task.TaskResponse;
import com.zubair.taskpulse.dto.task.UpdateTaskRequest;
import com.zubair.taskpulse.entity.Task;
import com.zubair.taskpulse.entity.TaskPriority;
import com.zubair.taskpulse.entity.TaskStatus;
import com.zubair.taskpulse.entity.User;
import com.zubair.taskpulse.repository.TaskRepository;
import com.zubair.taskpulse.repository.UserRepository;
import com.zubair.taskpulse.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Override
    public TaskResponse createTask(
            CreateTaskRequest request,
            String userEmail
    ) {

        User user = getUserByEmail(userEmail);

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(
                        request.getPriority() != null
                                ? request.getPriority()
                                : TaskPriority.MEDIUM
                )
                .status(TaskStatus.TODO)
                .deadline(request.getDeadline())
                .estimatedDurationMinutes(
                        request.getEstimatedDurationMinutes()
                )
                .user(user)
                .build();

        Task savedTask = taskRepository.save(task);

        return mapToResponse(savedTask);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasks(
            String userEmail,
            TaskStatus status,
            TaskPriority priority
    ) {

        User user = getUserByEmail(userEmail);

        List<Task> tasks;

        if (status != null && priority != null) {

            tasks = taskRepository
                    .findByUserIdAndStatusAndPriority(
                            user.getId(),
                            status,
                            priority
                    );

        } else if (status != null) {

            tasks = taskRepository
                    .findByUserIdAndStatus(
                            user.getId(),
                            status
                    );

        } else if (priority != null) {

            tasks = taskRepository
                    .findByUserIdAndPriority(
                            user.getId(),
                            priority
                    );

        } else {

            tasks = taskRepository
                    .findByUserId(user.getId());
        }

        return tasks.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTask(
            Long taskId,
            String userEmail
    ) {

        User user = getUserByEmail(userEmail);

        Task task = taskRepository
                .findByIdAndUserId(taskId, user.getId())
                .orElseThrow(() ->
                        new RuntimeException("Task not found")
                );

        return mapToResponse(task);
    }

    @Override
    public TaskResponse updateTask(
            Long taskId,
            UpdateTaskRequest request,
            String userEmail
    ) {

        User user = getUserByEmail(userEmail);

        Task task = taskRepository
                .findByIdAndUserId(taskId, user.getId())
                .orElseThrow(() ->
                        new RuntimeException("Task not found")
                );

        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }

        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }

        if (request.getStatus() != null) {

            task.setStatus(request.getStatus());

            if (request.getStatus() == TaskStatus.COMPLETED) {
                task.setCompletedAt(LocalDateTime.now());
            } else {
                task.setCompletedAt(null);
            }
        }

        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }

        if (request.getDeadline() != null) {
            task.setDeadline(request.getDeadline());
        }

        if (request.getEstimatedDurationMinutes() != null) {
            task.setEstimatedDurationMinutes(
                    request.getEstimatedDurationMinutes()
            );
        }

        Task updatedTask = taskRepository.save(task);

        return mapToResponse(updatedTask);
    }

    @Override
    public void deleteTask(
            Long taskId,
            String userEmail
    ) {

        User user = getUserByEmail(userEmail);

        Task task = taskRepository
                .findByIdAndUserId(taskId, user.getId())
                .orElseThrow(() ->
                        new RuntimeException("Task not found")
                );

        taskRepository.delete(task);
    }

    private User getUserByEmail(String email) {

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );
    }

    private TaskResponse mapToResponse(Task task) {

        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .deadline(task.getDeadline())
                .estimatedDurationMinutes(
                        task.getEstimatedDurationMinutes()
                )
                .completedAt(task.getCompletedAt())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
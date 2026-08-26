package com.zubair.taskpulse.service;

import com.zubair.taskpulse.dto.task.CreateTaskRequest;
import com.zubair.taskpulse.dto.task.TaskResponse;
import com.zubair.taskpulse.dto.task.UpdateTaskRequest;
import com.zubair.taskpulse.entity.TaskPriority;
import com.zubair.taskpulse.entity.TaskStatus;

import java.util.List;

public interface TaskService {

    TaskResponse createTask(
            CreateTaskRequest request,
            String userEmail
    );

    List<TaskResponse> getTasks(
            String userEmail,
            TaskStatus status,
            TaskPriority priority
    );

    TaskResponse getTask(
            Long taskId,
            String userEmail
    );

    TaskResponse updateTask(
            Long taskId,
            UpdateTaskRequest request,
            String userEmail
    );

    void deleteTask(
            Long taskId,
            String userEmail
    );
}
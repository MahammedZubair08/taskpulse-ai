package com.zubair.taskpulse.repository;

import com.zubair.taskpulse.entity.Task;
import com.zubair.taskpulse.entity.TaskPriority;
import com.zubair.taskpulse.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByUserId(Long userId);

    Optional<Task> findByIdAndUserId(Long taskId, Long userId);

    List<Task> findByUserIdAndStatus(
            Long userId,
            TaskStatus status
    );

    List<Task> findByUserIdAndPriority(
            Long userId,
            TaskPriority priority
    );

    List<Task> findByUserIdAndStatusAndPriority(
            Long userId,
            TaskStatus status,
            TaskPriority priority
    );

    boolean existsByIdAndUserId(Long taskId, Long userId);
}
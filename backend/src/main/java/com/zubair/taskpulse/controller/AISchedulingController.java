package com.zubair.taskpulse.controller;

import com.zubair.taskpulse.dto.scheduling.ApplyScheduleRequest;
import com.zubair.taskpulse.dto.scheduling.GenerateScheduleRequest;
import com.zubair.taskpulse.dto.scheduling.OptimizedScheduleResponse;
import com.zubair.taskpulse.entity.User;
import com.zubair.taskpulse.repository.UserRepository;
import com.zubair.taskpulse.service.AISchedulingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ai/schedule")
@RequiredArgsConstructor
public class AISchedulingController {

    private final AISchedulingService aiSchedulingService;
    private final UserRepository userRepository;

    @PostMapping("/generate")
    public ResponseEntity<OptimizedScheduleResponse> generateSchedule(
            @RequestBody(required = false) GenerateScheduleRequest request,
            Authentication authentication
    ) {
        User user = getUser(authentication);
        LocalDate targetDate = (request != null && request.targetDate() != null) ? request.targetDate() : LocalDate.now();
        OptimizedScheduleResponse schedule = aiSchedulingService.generateOptimizedSchedule(user, targetDate);
        return ResponseEntity.ok(schedule);
    }

    @PostMapping("/apply")
    public ResponseEntity<Map<String, Object>> applySchedule(
            @RequestBody ApplyScheduleRequest request,
            Authentication authentication
    ) {
        User user = getUser(authentication);
        int appliedCount = aiSchedulingService.applySchedule(user, request);
        return ResponseEntity.ok(Map.of("appliedCount", appliedCount, "message", "Successfully applied schedule to tasks!"));
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + authentication.getName()));
    }
}

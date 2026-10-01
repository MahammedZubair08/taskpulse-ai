package com.zubair.taskpulse.controller;

import com.zubair.taskpulse.dto.calendar.CalendarEventResponse;
import com.zubair.taskpulse.dto.calendar.ExportCalendarRequest;
import com.zubair.taskpulse.dto.calendar.ScheduleConflictResponse;
import com.zubair.taskpulse.dto.calendar.TimeSlotResponse;
import com.zubair.taskpulse.entity.User;
import com.zubair.taskpulse.repository.UserRepository;
import com.zubair.taskpulse.service.GoogleCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final GoogleCalendarService googleCalendarService;
    private final UserRepository userRepository;

    @GetMapping("/events")
    public ResponseEntity<List<CalendarEventResponse>> getEvents(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Authentication authentication
    ) {
        User user = getUser(authentication);
        List<CalendarEventResponse> events = googleCalendarService.fetchUpcomingEvents(user, from, to);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/conflicts")
    public ResponseEntity<List<ScheduleConflictResponse>> getConflicts(Authentication authentication) {
        User user = getUser(authentication);
        List<ScheduleConflictResponse> conflicts = googleCalendarService.detectConflicts(user);
        return ResponseEntity.ok(conflicts);
    }

    @GetMapping("/free-slots")
    public ResponseEntity<List<TimeSlotResponse>> getFreeSlots(
            @RequestParam(required = false, defaultValue = "60") Integer durationMinutes,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deadline,
            Authentication authentication
    ) {
        User user = getUser(authentication);
        List<TimeSlotResponse> slots = googleCalendarService.findFreeTimeSlots(user, durationMinutes, deadline);
        return ResponseEntity.ok(slots);
    }

    @PostMapping("/export/{taskId}")
    public ResponseEntity<CalendarEventResponse> exportTask(
            @PathVariable Long taskId,
            @RequestBody(required = false) ExportCalendarRequest request,
            Authentication authentication
    ) {
        User user = getUser(authentication);
        LocalDateTime startTime = request != null ? request.startTime() : null;
        CalendarEventResponse event = googleCalendarService.exportTaskToCalendar(user, taskId, startTime);
        return ResponseEntity.ok(event);
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + authentication.getName()));
    }
}

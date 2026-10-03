package com.zubair.taskpulse.controller;
import com.zubair.taskpulse.service.TelegramService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/telegram")
@RequiredArgsConstructor
public class TelegramController {
    private final TelegramService telegramService;
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody String update
    ) {
        telegramService.handleUpdate(update);
        return ResponseEntity.ok().build();
    }
}

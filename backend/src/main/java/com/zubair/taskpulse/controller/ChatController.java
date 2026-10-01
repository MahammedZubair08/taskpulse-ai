package com.zubair.taskpulse.controller;
import com.zubair.taskpulse.dto.chat.ChatRequest;
import com.zubair.taskpulse.dto.chat.ChatResponse;
import com.zubair.taskpulse.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;
    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        return ResponseEntity.ok(
                chatService.processMessage(
                        request,
                        userEmail
                )
        );
    }
}

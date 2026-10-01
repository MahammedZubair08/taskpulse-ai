package com.zubair.taskpulse.controller;

import com.zubair.taskpulse.dto.gmail.GmailConnectStatusResponse;
import com.zubair.taskpulse.dto.gmail.GmailSuggestionResponse;
import com.zubair.taskpulse.entity.User;
import com.zubair.taskpulse.repository.UserRepository;
import com.zubair.taskpulse.service.GmailOAuthService;
import com.zubair.taskpulse.service.GmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/gmail")
@RequiredArgsConstructor
public class GmailController {

    private final GmailOAuthService gmailOAuthService;
    private final GmailService gmailService;
    private final UserRepository userRepository;

    @GetMapping("/auth-url")
    public ResponseEntity<Map<String, String>> getAuthUrl(Authentication authentication) {
        String userEmail = authentication.getName();
        String url = gmailOAuthService.buildAuthorizationUrl(userEmail);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping(value = "/callback", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> handleCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String userEmail
    ) {
        try {
            gmailOAuthService.handleCallback(code, userEmail);

            String htmlResponse = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Gmail Connected - TaskPulse AI</title>
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100vh; background: #0f172a; color: #f8fafc; margin: 0; }
                        .card { background: #1e293b; padding: 40px; border-radius: 16px; box-shadow: 0 10px 25px rgba(0,0,0,0.5); text-align: center; max-width: 400px; border: 1px solid #334155; }
                        h2 { color: #38bdf8; margin-bottom: 12px; }
                        p { color: #94a3b8; font-size: 15px; margin-bottom: 24px; }
                        .checkmark { font-size: 48px; margin-bottom: 16px; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <div class="checkmark">✨</div>
                        <h2>Gmail Connected!</h2>
                        <p>TaskPulse AI is now linked with your Gmail inbox. You can close this window.</p>
                    </div>
                    <script>
                        if (window.opener) {
                            window.opener.postMessage("GMAIL_CONNECTED", "*");
                        }
                        setTimeout(() => { window.close(); }, 2500);
                    </script>
                </body>
                </html>
                """;

            return ResponseEntity.ok(htmlResponse);
        } catch (Exception e) {
            log.error("Error during Gmail callback", e);
            String errorHtml = """
                <!DOCTYPE html>
                <html>
                <head><title>Connection Failed</title></head>
                <body style="background:#0f172a; color:#ef4444; font-family:sans-serif; text-align:center; padding-top:50px;">
                    <h2>Failed to connect Gmail</h2>
                    <p>%s</p>
                </body>
                </html>
                """.formatted(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorHtml);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<GmailConnectStatusResponse> getStatus(Authentication authentication) {
        User user = getUser(authentication);
        boolean connected = gmailOAuthService.isConnected(user);
        return ResponseEntity.ok(new GmailConnectStatusResponse(connected, user.getEmail()));
    }

    @PostMapping("/sync")
    public ResponseEntity<com.zubair.taskpulse.dto.gmail.GmailSyncResultResponse> syncGmail(Authentication authentication) {
        User user = getUser(authentication);
        com.zubair.taskpulse.dto.gmail.GmailSyncResultResponse result = gmailService.fetchTaskSuggestions(user);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/disconnect")
    public ResponseEntity<Void> disconnect(Authentication authentication) {
        User user = getUser(authentication);
        gmailOAuthService.disconnect(user);
        return ResponseEntity.noContent().build();
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + authentication.getName()));
    }
}

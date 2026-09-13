package com.zubair.taskpulse.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zubair.taskpulse.config.GmailOAuthProperties;
import com.zubair.taskpulse.entity.GmailToken;
import com.zubair.taskpulse.entity.User;
import com.zubair.taskpulse.repository.GmailTokenRepository;
import com.zubair.taskpulse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GmailOAuthService {

    private final GmailOAuthProperties properties;
    private final GmailTokenRepository gmailTokenRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String buildAuthorizationUrl(String userEmail) {
        String state = URLEncoder.encode(userEmail, StandardCharsets.UTF_8);

        return "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=" + URLEncoder.encode(properties.getClientId(), StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(properties.getRedirectUri(), StandardCharsets.UTF_8) +
                "&response_type=code" +
                "&scope=" + URLEncoder.encode(properties.getScope(), StandardCharsets.UTF_8) +
                "&access_type=offline" +
                "&prompt=consent" +
                "&state=" + state;
    }

    @Transactional
    public void handleCallback(String code, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));

        Map<String, String> params = Map.of(
                "code", code,
                "client_id", properties.getClientId(),
                "client_secret", properties.getClientSecret(),
                "redirect_uri", properties.getRedirectUri(),
                "grant_type", "authorization_code"
        );

        String formBody = params.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Failed to exchange OAuth code for tokens: {}", response.body());
                throw new RuntimeException("Failed to authorize with Google: " + response.body());
            }

            JsonNode node = objectMapper.readTree(response.body());
            String accessToken = node.get("access_token").asText();
            int expiresIn = node.get("expires_in").asInt();
            String refreshToken = node.has("refresh_token") ? node.get("refresh_token").asText() : null;

            LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expiresIn);

            GmailToken gmailToken = gmailTokenRepository.findByUser(user)
                    .orElseGet(() -> GmailToken.builder().user(user).build());

            gmailToken.setAccessToken(accessToken);
            if (refreshToken != null && !refreshToken.isBlank()) {
                gmailToken.setRefreshToken(refreshToken);
            }
            gmailToken.setExpiresAt(expiresAt);

            gmailTokenRepository.save(gmailToken);
            log.info("Successfully saved Gmail token for user {}", userEmail);

        } catch (Exception e) {
            log.error("Error during Google OAuth callback processing", e);
            throw new RuntimeException("Failed to exchange OAuth token", e);
        }
    }

    @Transactional
    public String getValidAccessToken(User user) {
        GmailToken token = gmailTokenRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Gmail account not connected"));

        if (!token.isExpired()) {
            return token.getAccessToken();
        }

        log.info("Gmail access token expired for user {}, refreshing...", user.getEmail());

        if (token.getRefreshToken() == null) {
            throw new IllegalStateException("No refresh token available. User must reconnect Gmail.");
        }

        return refreshAccessToken(token);
    }

    private String refreshAccessToken(GmailToken token) {
        Map<String, String> params = Map.of(
                "client_id", properties.getClientId(),
                "client_secret", properties.getClientSecret(),
                "refresh_token", token.getRefreshToken(),
                "grant_type", "refresh_token"
        );

        String formBody = params.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Failed to refresh Gmail access token: {}", response.body());
                throw new RuntimeException("Failed to refresh Gmail token");
            }

            JsonNode node = objectMapper.readTree(response.body());
            String newAccessToken = node.get("access_token").asText();
            int expiresIn = node.get("expires_in").asInt();

            token.setAccessToken(newAccessToken);
            token.setExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));
            gmailTokenRepository.save(token);

            return newAccessToken;

        } catch (Exception e) {
            log.error("Error refreshing token for user {}", token.getUser().getEmail(), e);
            throw new RuntimeException("Token refresh failed", e);
        }
    }

    public boolean isConnected(User user) {
        Optional<GmailToken> token = gmailTokenRepository.findByUser(user);
        return token.isPresent() && token.get().getRefreshToken() != null;
    }

    @Transactional
    public void disconnect(User user) {
        gmailTokenRepository.deleteByUser(user);
        log.info("Disconnected Gmail for user {}", user.getEmail());
    }
}

package com.zubair.taskpulse.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "google.oauth2")
public class GmailOAuthProperties {

    private String clientId;
    private String clientSecret;
    private String redirectUri = "http://localhost:8080/api/gmail/callback";
    private String scope = "https://www.googleapis.com/auth/gmail.readonly";
}

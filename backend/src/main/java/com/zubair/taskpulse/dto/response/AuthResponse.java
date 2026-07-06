package com.zubair.taskpulse.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for authentication response.
 * 
 * Returned after successful login or registration.
 * Contains the JWT token for subsequent API requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /**
     * JWT access token for authentication.
     * Must be included in the Authorization header as "Bearer {token}".
     */
    @JsonProperty("token")
    private String token;

    /**
     * Type of token (always "Bearer").
     */
    @JsonProperty("type")
    private String type;

    /**
     * Expiration time of the token in milliseconds.
     */
    @JsonProperty("expiresIn")
    private Long expiresIn;

    /**
     * User's ID.
     */
    @JsonProperty("userId")
    private Long userId;

    /**
     * User's email address.
     */
    @JsonProperty("email")
    private String email;

    /**
     * User's first name.
     */
    @JsonProperty("firstName")
    private String firstName;

    /**
     * User's last name.
     */
    @JsonProperty("lastName")
    private String lastName;

    /**
     * User's role in the system.
     */
    @JsonProperty("role")
    private String role;
}

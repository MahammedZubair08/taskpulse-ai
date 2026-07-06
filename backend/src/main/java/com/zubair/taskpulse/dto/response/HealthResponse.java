package com.zubair.taskpulse.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for health check endpoint.
 * 
 * This DTO encapsulates the health status information returned by the application.
 * It follows the DTO pattern to decouple the API layer from internal data structures.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthResponse {

    /**
     * The health status of the application (e.g., "UP", "DOWN").
     */
    @JsonProperty("status")
    private String status;

    /**
     * The application name.
     */
    @JsonProperty("application")
    private String application;

    /**
     * The current version of the application.
     */
    @JsonProperty("version")
    private String version;

    /**
     * The current timestamp in ISO-8601 format.
     */
    @JsonProperty("timestamp")
    private String timestamp;
}

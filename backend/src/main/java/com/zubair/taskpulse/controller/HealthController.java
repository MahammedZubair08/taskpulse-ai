package com.zubair.taskpulse.controller;

import com.zubair.taskpulse.dto.response.HealthResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * REST Controller for health check endpoint.
 * 
 * This controller provides a simple health check mechanism to verify that the application
 * is running and responding to requests. It follows the REST architectural style and
 * returns standardized JSON responses.
 * 
 * Architecture Decision:
 * - No service layer is used for this simple health check as it adds unnecessary complexity.
 * - Response is built directly in the controller since no business logic is involved.
 * - The endpoint is public and unauthenticated to allow monitoring systems access.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private static final String APPLICATION_NAME = "TaskPulse AI";
    private static final String APPLICATION_VERSION = "1.0.0";
    private static final String HEALTH_STATUS_UP = "UP";
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Health check endpoint.
     * 
     * Returns the current health status of the application along with metadata.
     * This endpoint is useful for monitoring, load balancing, and deployment verification.
     * 
     * @return ResponseEntity containing HealthResponse with HTTP 200 status
     */
    @GetMapping
    public ResponseEntity<HealthResponse> health() {
        HealthResponse healthResponse = HealthResponse.builder()
                .status(HEALTH_STATUS_UP)
                .application(APPLICATION_NAME)
                .version(APPLICATION_VERSION)
                .timestamp(LocalDateTime.now().format(ISO_FORMATTER))
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(healthResponse);
    }
}

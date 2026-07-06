package com.zubair.taskpulse.controller;

import com.zubair.taskpulse.dto.request.LoginRequest;
import com.zubair.taskpulse.dto.request.RegisterRequest;
import com.zubair.taskpulse.dto.response.ApiResponse;
import com.zubair.taskpulse.dto.response.AuthResponse;
import com.zubair.taskpulse.service.impl.AuthenticationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for authentication endpoints.
 * 
 * Handles user registration and login operations.
 * Both endpoints return JWT tokens for subsequent API requests.
 * 
 * Endpoints:
 * - POST /api/auth/register - Register a new user
 * - POST /api/auth/login - Authenticate user and return JWT token
 * 
 * These endpoints are publicly accessible without prior authentication.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    /**
     * Construct auth controller with service dependency.
     * 
     * @param authenticationService authentication service
     */
    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * Register a new user in the system.
     * 
     * Endpoint: POST /api/auth/register
     * 
     * Request body example:
     * {
     *   "firstName": "John",
     *   "lastName": "Doe",
     *   "email": "john.doe@example.com",
     *   "password": "SecurePassword123"
     * }
     * 
     * Response: HTTP 201 Created with authentication token
     * {
     *   "status": 201,
     *   "message": "User registered successfully",
     *   "success": true,
     *   "data": {
     *     "token": "eyJhbGc...",
     *     "type": "Bearer",
     *     "expiresIn": 86400000,
     *     "userId": 1,
     *     "email": "john.doe@example.com",
     *     "firstName": "John",
     *     "lastName": "Doe",
     *     "role": "USER"
     *   },
     *   "timestamp": 1720291200000
     * }
     * 
     * @param registerRequest registration details
     * @return ResponseEntity with auth response
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("User registration request for email: {}", registerRequest.getEmail());

        AuthResponse authResponse = authenticationService.register(registerRequest);

        log.info("User registered successfully: {}", registerRequest.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", authResponse));
    }

    /**
     * Authenticate user and return JWT token.
     * 
     * Endpoint: POST /api/auth/login
     * 
     * Request body example:
     * {
     *   "email": "john.doe@example.com",
     *   "password": "SecurePassword123"
     * }
     * 
     * Response: HTTP 200 OK with authentication token
     * {
     *   "status": 200,
     *   "message": "Login successful",
     *   "success": true,
     *   "data": {
     *     "token": "eyJhbGc...",
     *     "type": "Bearer",
     *     "expiresIn": 86400000,
     *     "userId": 1,
     *     "email": "john.doe@example.com",
     *     "firstName": "John",
     *     "lastName": "Doe",
     *     "role": "USER"
     *   },
     *   "timestamp": 1720291200000
     * }
     * 
     * @param loginRequest login credentials
     * @return ResponseEntity with auth response
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("User login request for email: {}", loginRequest.getEmail());

        AuthResponse authResponse = authenticationService.login(loginRequest);

        log.info("User logged in successfully: {}", loginRequest.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }
}

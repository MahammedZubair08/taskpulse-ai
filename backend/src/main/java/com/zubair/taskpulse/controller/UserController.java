package com.zubair.taskpulse.controller;

import com.zubair.taskpulse.dto.response.ApiResponse;
import com.zubair.taskpulse.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for user-related endpoints.
 * 
 * These endpoints demonstrate JWT token validation.
 * All endpoints require a valid JWT token in the Authorization header.
 * 
 * Endpoints:
 * - GET /api/users/me - Get current user information
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    /**
     * Construct user controller with repository dependency.
     * 
     * @param userRepository user data repository
     */
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Get current authenticated user information.
     * 
     * Endpoint: GET /api/users/me
     * 
     * This endpoint demonstrates JWT token validation.
     * The principal contains the email of the authenticated user.
     * 
     * Example Response (HTTP 200):
     * {
     *   "status": 200,
     *   "message": "User information retrieved",
     *   "success": true,
     *   "data": {
     *     "id": 1,
     *     "firstName": "John",
     *     "lastName": "Doe",
     *     "email": "john.doe@example.com",
     *     "role": "USER"
     *   },
     *   "timestamp": 1720291200000
     * }
     * 
     * @param authentication Spring Security authentication object
     * @return ResponseEntity with user information
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser(Authentication authentication) {
        log.debug("Getting current user info for: {}", authentication.getName());

        return userRepository.findByEmail(authentication.getName())
                .map(user -> {
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", user.getId());
                    userInfo.put("firstName", user.getFirstName());
                    userInfo.put("lastName", user.getLastName());
                    userInfo.put("email", user.getEmail());
                    userInfo.put("role", user.getRole().name());

                    return ResponseEntity.ok(ApiResponse.success("User information retrieved", userInfo));
                })
                .orElseGet(() -> ResponseEntity.ok(
                        ApiResponse.error(404, "User not found")
                ));
    }
}

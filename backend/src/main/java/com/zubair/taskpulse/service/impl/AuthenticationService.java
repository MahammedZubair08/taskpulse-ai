package com.zubair.taskpulse.service.impl;

import com.zubair.taskpulse.dto.request.LoginRequest;
import com.zubair.taskpulse.dto.request.RegisterRequest;
import com.zubair.taskpulse.dto.response.AuthResponse;
import com.zubair.taskpulse.entity.User;
import com.zubair.taskpulse.entity.UserRole;
import com.zubair.taskpulse.exception.ApplicationException;
import com.zubair.taskpulse.repository.UserRepository;
import com.zubair.taskpulse.security.jwt.JwtService;
import com.zubair.taskpulse.service.IService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication service implementation.
 * 
 * Handles user registration and login operations.
 * 
 * Responsibilities:
 * - Validate registration input and check for duplicate emails
 * - Encrypt passwords using BCrypt
 * - Authenticate user credentials
 * - Generate and return JWT tokens
 * 
 * All operations are transactional to ensure data consistency.
 */
@Slf4j
@Service
@Transactional
public class AuthenticationService implements IService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /**
     * Construct authentication service with dependencies.
     * 
     * @param userRepository user data repository
     * @param passwordEncoder password encoding service
     * @param authenticationManager authentication manager
     * @param jwtService JWT token service
     */
    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * Register a new user in the system.
     * 
     * Process:
     * 1. Validate input using Bean Validation
     * 2. Check if email already exists
     * 3. Encode password using BCrypt
     * 4. Save user entity to database
     * 5. Generate JWT token
     * 6. Return authentication response
     * 
     * @param registerRequest user registration details
     * @return AuthResponse with JWT token
     * @throws ApplicationException if email already exists or validation fails
     */
    public AuthResponse register(RegisterRequest registerRequest) {
        // Check if email already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            log.warn("Registration failed: email already exists - {}", registerRequest.getEmail());
            throw new ApplicationException("Email already exists");
        }

        // Create new user entity
        User user = User.builder()
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role(UserRole.USER)  // New users get USER role by default
                .build();

        // Save user to database
        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getEmail());

        // Generate JWT token
        String token = jwtService.generateToken(savedUser.getId(), savedUser.getEmail(), savedUser.getRole().name());

        // Build and return authentication response
        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .expiresIn(jwtService.getExpiration())
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .role(savedUser.getRole().name())
                .build();
    }

    /**
     * Authenticate user and return JWT token.
     * 
     * Process:
     * 1. Verify email and password are correct
     * 2. Load user from database
     * 3. Generate JWT token with user claims
     * 4. Return authentication response
     * 
     * @param loginRequest user login credentials
     * @return AuthResponse with JWT token
     * @throws ApplicationException if credentials are invalid
     */
    public AuthResponse login(LoginRequest loginRequest) {
        try {
            // Authenticate using Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            // Get authenticated user details
            String email = authentication.getName();

            // Load user from database
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ApplicationException("User not found"));

            log.info("User logged in successfully: {}", email);

            // Generate JWT token
            String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());

            // Build and return authentication response
            return AuthResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .expiresIn(jwtService.getExpiration())
                    .userId(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .role(user.getRole().name())
                    .build();

        } catch (AuthenticationException e) {
            log.warn("Login failed: invalid credentials for email - {}", loginRequest.getEmail());
            throw new ApplicationException("Invalid email or password");
        }
    }
}

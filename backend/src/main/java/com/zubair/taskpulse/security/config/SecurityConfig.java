package com.zubair.taskpulse.security.config;

import com.zubair.taskpulse.security.filter.JwtAuthenticationFilter;
import com.zubair.taskpulse.security.jwt.JwtService;
import com.zubair.taskpulse.security.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for TaskPulse AI application.
 * 
 * Configures Spring Security with JWT-based stateless authentication.
 * 
 * Security Flow:
 * 1. Public endpoints (/api/auth/**, /api/health) are accessible without authentication
 * 2. All other endpoints require a valid JWT token
 * 3. JWT token is extracted from Authorization: Bearer <token> header
 * 4. JwtAuthenticationFilter validates the token and sets authentication context
 * 5. Authorization checks are performed based on user roles
 * 
 * Session Management: Stateless (JWT-based)
 * Password Encoding: BCrypt
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configure HTTP security for the application.
     * 
     * Public endpoints (no authentication required):
     * - POST /api/auth/register - User registration
     * - POST /api/auth/login - User login
     * - GET /api/health - Health check
     * - GET /actuator/health - Actuator health
     * - GET /actuator/info - Actuator info
     * 
     * Protected endpoints (JWT token required):
     * - All other endpoints
     * 
     * @param http HttpSecurity configuration
     * @param jwtService JWT service for token validation
     * @return SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints for authentication and monitoring
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configure authentication provider for database-backed authentication.
     * 
     * Uses CustomUserDetailsService to load user details from database
     * and BCryptPasswordEncoder to verify passwords.
     * 
     * @param userDetailsService custom user details service
     * @param passwordEncoder password encoder
     * @return AuthenticationProvider
     */
    @Bean
    public AuthenticationProvider authenticationProvider(
            CustomUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /**
     * Password encoder for user credentials.
     * 
     * Uses BCrypt algorithm with default strength (10) for encoding passwords.
     * BCrypt is adaptive and will automatically increase strength over time.
     * 
     * @return PasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Authentication manager for the application.
     * 
     * Used by AuthenticationService for authenticating user credentials.
     * 
     * @param authConfig AuthenticationConfiguration
     * @return AuthenticationManager instance
     * @throws Exception if configuration fails
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}

package com.zubair.taskpulse.exception;

import com.zubair.taskpulse.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for all REST endpoints.
 * 
 * Catches exceptions across the entire application and returns
 * consistent JSON error responses.
 * 
 * Handled Exceptions:
 * - ApplicationException: Custom business logic exceptions
 * - MethodArgumentNotValidException: Bean validation errors
 * - AuthenticationException: Security/authentication errors
 * - Generic Exception: Unexpected errors
 * 
 * All responses follow the ApiResponse wrapper format.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle ApplicationException.
     * 
     * Called when business logic errors occur (e.g., duplicate email, user not found).
     * Returns HTTP 400 Bad Request.
     * 
     * @param e ApplicationException
     * @param request request details
     * @return ApiResponse with error details
     */
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiResponse<Object>> handleApplicationException(
            ApplicationException e,
            WebRequest request) {

        log.warn("Application exception occurred: {}", e.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                e.getMessage()
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle validation errors from Bean Validation.
     * 
     * Called when @Valid validation fails on request parameters.
     * Returns HTTP 400 Bad Request with detailed field errors.
     * 
     * @param e MethodArgumentNotValidException
     * @param request request details
     * @return ApiResponse with validation error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(
            MethodArgumentNotValidException e,
            WebRequest request) {

        log.warn("Validation error occurred: {}", e.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        ApiResponse<Object> response = ApiResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                fieldErrors
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle authentication and security errors.
     * 
     * Called when authentication fails or security constraints are violated.
     * Returns HTTP 401 Unauthorized.
     * 
     * @param e AuthenticationException
     * @param request request details
     * @return ApiResponse with authentication error
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(
            AuthenticationException e,
            WebRequest request) {

        log.warn("Authentication error occurred: {}", e.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
                HttpStatus.UNAUTHORIZED.value(),
                "Authentication failed: " + e.getMessage()
        );

        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handle generic exceptions.
     * 
     * Catches all other unexpected exceptions.
     * Returns HTTP 500 Internal Server Error.
     * Logs the full exception for debugging.
     * 
     * @param e Exception
     * @param request request details
     * @return ApiResponse with error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(
            Exception e,
            WebRequest request) {

        log.error("Unexpected exception occurred", e);

        ApiResponse<Object> response = ApiResponse.error(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred. Please try again later."
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

package com.zubair.taskpulse.exception;

/**
 * Base exception class for all application-specific exceptions.
 * 
 * This exception serves as the root for all custom exceptions in the application,
 * allowing for a consistent exception handling strategy across the entire system.
 * 
 * Architecture Pattern: Custom Exception Hierarchy
 * - Enables targeted exception handling
 * - Allows differentiation from third-party library exceptions
 * - Supports custom error messages and codes
 */
public class ApplicationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs an ApplicationException with the specified detail message.
     *
     * @param message the detail message
     */
    public ApplicationException(String message) {
        super(message);
    }

    /**
     * Constructs an ApplicationException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of the exception
     */
    public ApplicationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs an ApplicationException with the specified cause.
     *
     * @param cause the cause of the exception
     */
    public ApplicationException(Throwable cause) {
        super(cause);
    }
}

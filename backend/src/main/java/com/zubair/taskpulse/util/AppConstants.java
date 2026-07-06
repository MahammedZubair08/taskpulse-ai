package com.zubair.taskpulse.util;

/**
 * Centralized constants used throughout the application.
 * 
 * This class provides a single source of truth for application-wide constants,
 * improving maintainability and reducing magic strings across the codebase.
 * 
 * Best Practice: Keep constants organized by functional area.
 */
public final class AppConstants {

    // Private constructor to prevent instantiation
    private AppConstants() {
        throw new UnsupportedOperationException("AppConstants cannot be instantiated");
    }

    // API Versioning
    public static final String API_BASE_PATH = "/api";
    public static final String API_V1_PATH = "/api/v1";

    // Application Metadata
    public static final String APPLICATION_NAME = "TaskPulse AI";
    public static final String APPLICATION_VERSION = "1.0.0";
    public static final String APPLICATION_DESCRIPTION = "Intelligent Task Management System with AI Integration";

    // HTTP Headers
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";

    // Security Constants
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_USER = "ROLE_USER";
}

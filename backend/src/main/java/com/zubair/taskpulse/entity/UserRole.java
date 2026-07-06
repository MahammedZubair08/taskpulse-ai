package com.zubair.taskpulse.entity;

/**
 * Enum representing user roles in the system.
 * 
 * Used for role-based access control (RBAC).
 * Roles determine what endpoints and resources a user can access.
 * 
 * USER - Regular application user with standard permissions
 * ADMIN - Administrator with elevated privileges
 */
public enum UserRole {
    /**
     * Regular user with standard application access.
     */
    USER,

    /**
     * Administrator with full system access.
     */
    ADMIN
}

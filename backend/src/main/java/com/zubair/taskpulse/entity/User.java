package com.zubair.taskpulse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * User entity representing an application user.
 * 
 * This entity stores user account information including authentication credentials.
 * Timestamps are automatically managed by Hibernate for audit purposes.
 * 
 * Architecture:
 * - Extends BaseEntity (planned for future implementation)
 * - Uses Lombok for boilerplate reduction
 * - Uses Hibernate for automatic timestamp management
 * - Email is unique constraint for user identification
 * - Password is stored as BCrypt hash (never plain text)
 */
@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = "email", name = "uk_user_email")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /**
     * Unique identifier for the user.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * User's first name.
     */
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    /**
     * User's last name.
     */
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /**
     * User's email address (unique).
     * Used for login authentication.
     */
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    /**
     * User's password hash (BCrypt encrypted).
     * Never stored or transmitted in plain text.
     */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    /**
     * User's role in the system.
     * Determines authorization level and permissions.
     * 
     * Allowed values: USER, ADMIN
     */
    @Column(name = "role", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private UserRole role;

    /**
     * Timestamp when the user was created.
     * Automatically set by Hibernate on entity creation.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the user was last updated.
     * Automatically updated by Hibernate on entity modification.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

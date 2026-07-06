package com.zubair.taskpulse.repository;

import com.zubair.taskpulse.entity.User;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User entity.
 * 
 * Provides database access methods for User entity operations.
 * Extends BaseRepository for inherited CRUD operations.
 * 
 * Additional Methods:
 * - findByEmail: Retrieve user by email address
 * - existsByEmail: Check if email already exists
 */
@Repository
public interface UserRepository extends BaseRepository<User, Long> {

    /**
     * Find a user by their email address.
     * 
     * @param email the email address to search for
     * @return Optional containing the user if found, empty otherwise
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user with the given email exists in the database.
     * 
     * @param email the email address to check
     * @return true if user exists, false otherwise
     */
    boolean existsByEmail(String email);
}

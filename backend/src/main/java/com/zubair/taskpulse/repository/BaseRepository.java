package com.zubair.taskpulse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Base repository interface for all domain entities.
 * 
 * This generic interface extends JpaRepository and provides common CRUD operations
 * for all repository implementations. It follows the Repository pattern from DDD
 * (Domain-Driven Design) principles.
 * 
 * Architecture Pattern: Repository Pattern + Generic Abstraction
 * - Provides consistent data access layer interface
 * - Reduces boilerplate code in repository implementations
 * - Enables easy switching between different data sources
 * 
 * @param <T> the entity type
 * @param <ID> the entity ID type
 */
@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID> {
    // Base repository with common CRUD operations inherited from JpaRepository
}

package com.kaczmarek.documentservice.repository;

import com.kaczmarek.documentservice.domain.Document;
import com.kaczmarek.documentservice.dto.DocumentSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    
    /**
     * NAIVE VERSION: Returns entities with default lazy loading.
     * This will cause N+1 queries when accessing relationships.
     */
    // Using default findAll() - no fetch joins, lazy loading
    
    /**
     * OPTIMIZED VERSION 1: Using Entity Graph to eagerly fetch Author.
     * This prevents N+1 queries for the author relationship.
     */
    @org.springframework.data.jpa.repository.EntityGraph(
        attributePaths = {"author"},
        type = EntityGraphType.FETCH
    )
    @Query("SELECT d FROM Document d")
    Page<Document> findAllWithAuthor(Pageable pageable);
    
    /**
     * OPTIMIZED VERSION 2: Using DTO projection with explicit fetch join.
     * This is the most efficient approach - only fetches needed fields.
     */
    @Query("""
        SELECT new com.kaczmarek.documentservice.dto.DocumentSummaryDTO(
            d.id, d.title, d.createdAt, a.name, a.id
        )
        FROM Document d
        JOIN FETCH d.author a
        ORDER BY d.createdAt DESC
        """)
    Page<DocumentSummaryDTO> findAllSummaries(Pageable pageable);
    
    /**
     * OPTIMIZED VERSION: Fetch document with all relationships for detail view.
     * Uses Entity Graph to eagerly fetch all related entities in a single query.
     */
    @org.springframework.data.jpa.repository.EntityGraph(
        attributePaths = {"author", "tags", "permissions"},
        type = EntityGraphType.FETCH
    )
    @Query("SELECT d FROM Document d WHERE d.id = :id")
    Optional<Document> findByIdWithRelations(@Param("id") Long id);
    
    /**
     * Alternative: Using explicit JOIN FETCH for detail view.
     * This ensures all relationships are loaded in a single query.
     */
    @Query("""
        SELECT DISTINCT d FROM Document d
        LEFT JOIN FETCH d.author
        LEFT JOIN FETCH d.tags
        LEFT JOIN FETCH d.permissions
        WHERE d.id = :id
        """)
    Optional<Document> findByIdWithAllRelations(@Param("id") Long id);
}


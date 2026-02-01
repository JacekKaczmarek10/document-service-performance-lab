package com.kaczmarek.documentservice.service;

import com.kaczmarek.documentservice.domain.Document;
import com.kaczmarek.documentservice.domain.Permission;
import com.kaczmarek.documentservice.dto.DocumentDetailDTO;
import com.kaczmarek.documentservice.dto.DocumentSummaryDTO;
import com.kaczmarek.documentservice.repository.DocumentRepository;
import com.kaczmarek.documentservice.util.QueryMetricsCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * Service layer for document operations.
 * Provides both naive and optimized implementations for performance comparison.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {
    
    private final DocumentRepository documentRepository;
    private final QueryMetricsCollector metricsCollector;
    
    /**
     * NAIVE IMPLEMENTATION: Returns entities directly with lazy loading.
     * 
     * PROBLEMS:
     * - No pagination (loads all documents)
     * - Lazy loading causes N+1 queries when accessing relationships
     * - Returns full entity objects (unnecessary data)
     * - No fetch strategy (each relationship triggers separate query)
     * 
     * EXPECTED BEHAVIOR:
     * - 1 query to fetch documents
     * - N queries to fetch authors (N+1 problem)
     * - N queries to fetch tags (if accessed)
     * - N queries to fetch permissions (if accessed)
     * 
     * For 100 documents: ~1 + 100 + 100 + 100 = 301 queries (worst case)
     */
    @Transactional(readOnly = true)
    public Page<Document> findAllNaive(Pageable pageable) {
        log.warn("Using NAIVE implementation - expect N+1 queries!");
        QueryMetricsCollector.QueryStats before = metricsCollector.captureStats();
        
        // This will trigger lazy loading for each document's author
        Page<Document> documents = documentRepository.findAll(pageable);
        
        // Accessing author.name triggers N+1 queries
        documents.forEach(doc -> {
            // This line causes N+1 query problem!
            String authorName = doc.getAuthor().getName();
        });
        
        QueryMetricsCollector.QueryStats after = metricsCollector.captureStats();
        QueryMetricsCollector.QueryStats diff = metricsCollector.diff(before, after);
        metricsCollector.logStats("GET /documents (naive)", diff);
        
        return documents;
    }
    
    /**
     * OPTIMIZED IMPLEMENTATION: Uses DTO projection with fetch joins.
     * 
     * OPTIMIZATIONS APPLIED:
     * 1. DTO Projection: Only fetches required fields (reduces payload size)
     * 2. JOIN FETCH: Eagerly fetches author in same query (eliminates N+1)
     * 3. Pagination: Limits result set size
     * 4. Caching: Optional caching layer for frequently accessed data
     * 
     * EXPECTED BEHAVIOR:
     * - 1 query with JOIN FETCH to get all data in single query
     * - Minimal payload (only DTO fields)
     * 
     * For 100 documents (page size 20): 1 query total
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "documents", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<DocumentSummaryDTO> findAllOptimized(Pageable pageable) {
        log.info("Using OPTIMIZED implementation with DTO projection and fetch joins");
        QueryMetricsCollector.QueryStats before = metricsCollector.captureStats();
        
        // Default sorting by creation date (most recent first)
        Pageable sortedPageable = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
        
        // DTO projection with explicit JOIN FETCH eliminates N+1 queries
        Page<DocumentSummaryDTO> summaries = documentRepository.findAllSummaries(sortedPageable);
        
        QueryMetricsCollector.QueryStats after = metricsCollector.captureStats();
        QueryMetricsCollector.QueryStats diff = metricsCollector.diff(before, after);
        metricsCollector.logStats("GET /documents/optimized", diff);
        
        return summaries;
    }
    
    /**
     * OPTIMIZED: Fetch document with all relationships using Entity Graph.
     * This ensures all related data is loaded in a single query.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "documentDetails", key = "#id")
    public DocumentDetailDTO findByIdOptimized(Long id) {
        log.info("Fetching document {} with optimized query", id);
        QueryMetricsCollector.QueryStats before = metricsCollector.captureStats();
        
        Document document = documentRepository.findByIdWithAllRelations(id)
            .orElseThrow(() -> new RuntimeException("Document not found: " + id));
        
        // Map to DTO (all relationships already loaded, no additional queries)
        DocumentDetailDTO dto = DocumentDetailDTO.builder()
            .id(document.getId())
            .title(document.getTitle())
            .createdAt(document.getCreatedAt())
            .author(DocumentDetailDTO.AuthorDTO.builder()
                .id(document.getAuthor().getId())
                .name(document.getAuthor().getName())
                .build())
            .tagNames(document.getTags().stream()
                .map(tag -> tag.getName())
                .collect(Collectors.toSet()))
            .permissionTypes(document.getPermissions().stream()
                .map(Permission::getType)
                .map(Enum::name)
                .collect(Collectors.toList()))
            .build();
        
        QueryMetricsCollector.QueryStats after = metricsCollector.captureStats();
        QueryMetricsCollector.QueryStats diff = metricsCollector.diff(before, after);
        metricsCollector.logStats("GET /documents/" + id, diff);
        
        return dto;
    }
}


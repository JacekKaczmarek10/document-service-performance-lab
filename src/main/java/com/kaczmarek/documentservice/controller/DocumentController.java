package com.kaczmarek.documentservice.controller;

import com.kaczmarek.documentservice.domain.Document;
import com.kaczmarek.documentservice.dto.DocumentDetailDTO;
import com.kaczmarek.documentservice.dto.DocumentSummaryDTO;
import com.kaczmarek.documentservice.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for document endpoints.
 * Provides both naive and optimized implementations for performance comparison.
 */
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {
    
    private final DocumentService documentService;
    
    /**
     * NAIVE ENDPOINT: Returns entities directly.
     * 
     * This endpoint demonstrates the N+1 query problem.
     * 
     * Usage: GET /documents?page=0&size=20
     * 
     * Expected issues:
     * - Multiple queries per document (N+1 problem)
     * - Large payload (full entity objects)
     * - No pagination by default (if not specified)
     */
    @GetMapping
    public ResponseEntity<Page<Document>> getAllDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Document> documents = documentService.findAllNaive(pageable);
        return ResponseEntity.ok(documents);
    }
    
    /**
     * OPTIMIZED ENDPOINT: Returns DTOs with efficient fetching.
     * 
     * This endpoint demonstrates optimized query patterns.
     * 
     * Usage: GET /documents/optimized?page=0&size=20
     * 
     * Optimizations:
     * - DTO projection (minimal payload)
     * - JOIN FETCH (single query)
     * - Pagination support
     * - Optional caching
     */
    @GetMapping("/optimized")
    public ResponseEntity<Page<DocumentSummaryDTO>> getAllDocumentsOptimized(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<DocumentSummaryDTO> summaries = documentService.findAllOptimized(pageable);
        return ResponseEntity.ok(summaries);
    }
    
    /**
     * DETAIL ENDPOINT: Returns single document with all relationships.
     * 
     * Uses Entity Graph to fetch all relationships in a single query.
     * 
     * Usage: GET /documents/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentDetailDTO> getDocumentById(@PathVariable Long id) {
        DocumentDetailDTO document = documentService.findByIdOptimized(id);
        return ResponseEntity.ok(document);
    }
}


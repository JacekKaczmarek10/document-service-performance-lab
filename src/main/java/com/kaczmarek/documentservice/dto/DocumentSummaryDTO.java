package com.kaczmarek.documentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Lightweight DTO for document list endpoint.
 * Only includes essential fields to minimize payload size.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSummaryDTO {
    private Long id;
    private String title;
    private LocalDateTime createdAt;
    private String authorName;
    private Long authorId;
}


package com.kaczmarek.documentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * DTO for document detail endpoint.
 * Includes all related entities but fetched efficiently.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDetailDTO {
    private Long id;
    private String title;
    private LocalDateTime createdAt;
    private AuthorDTO author;
    private Set<String> tagNames;
    private List<String> permissionTypes;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorDTO {
        private Long id;
        private String name;
    }
}


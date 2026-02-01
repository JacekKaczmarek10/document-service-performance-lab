package com.kaczmarek.documentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO to expose query metrics for performance analysis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryMetricsDTO {
    private int queryCount;
    private long executionTimeMs;
    private String endpoint;
}


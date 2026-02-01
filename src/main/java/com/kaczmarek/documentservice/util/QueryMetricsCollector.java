package com.kaczmarek.documentservice.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.stereotype.Component;

/**
 * Utility class to collect and log Hibernate query statistics.
 * Helps identify N+1 query problems and measure optimization impact.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueryMetricsCollector {
    
    private final SessionFactory sessionFactory;
    
    /**
     * Captures query statistics snapshot.
     */
    public QueryStats captureStats() {
        Statistics stats = sessionFactory.getStatistics();
        return new QueryStats(
            stats.getQueryExecutionCount(),
            stats.getEntityLoadCount(),
            stats.getCollectionLoadCount(),
            stats.getQueryExecutionMaxTime()
        );
    }
    
    /**
     * Calculates the difference between two stats snapshots.
     */
    public QueryStats diff(QueryStats before, QueryStats after) {
        return new QueryStats(
            after.queryCount - before.queryCount,
            after.entityLoadCount - before.entityLoadCount,
            after.collectionLoadCount - before.collectionLoadCount,
            after.maxQueryTime - before.maxQueryTime
        );
    }
    
    /**
     * Logs query statistics in a readable format.
     */
    public void logStats(String endpoint, QueryStats stats) {
        log.info("=== Query Metrics for {} ===", endpoint);
        log.info("Total Queries Executed: {}", stats.queryCount);
        log.info("Entities Loaded: {}", stats.entityLoadCount);
        log.info("Collections Loaded: {}", stats.collectionLoadCount);
        log.info("Max Query Time: {} ms", stats.maxQueryTime);
        log.info("=====================================");
    }
    
    public record QueryStats(
        long queryCount,
        long entityLoadCount,
        long collectionLoadCount,
        long maxQueryTime
    ) {}
}


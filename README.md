# Document Service Performance Lab

A Spring Boot application demonstrating Hibernate/JPA performance optimization techniques, specifically focusing on solving the N+1 query problem.

## Overview

This project serves as a case study showing how a naive, poorly-optimized endpoint was analyzed and optimized step by step. The focus is on measurable performance improvements through proper JPA query strategies.

## Tech Stack

- **Java 25**
- **Spring Boot 4.0.01**
- **Spring Data JPA** (Hibernate)
- **PostgreSQL 16**
- **Flyway** (database migrations)
- **Maven**
- **Docker Compose** (PostgreSQL)
- **Caffeine Cache** (optional caching layer)

## Domain Model

The application models a simple document management system:

- **Document** (id, title, createdAt)
- **Author** (id, name) - ManyToOne relationship with Document
- **Tag** (id, name) - ManyToMany relationship with Document
- **Permission** (id, type) - OneToMany relationship with Document

## Getting Started

### Prerequisites

- Java 17+ (LTS recommended - Java 17 or 21)
- Maven 3.6+
- Docker and Docker Compose

### Running the Application

1. **Start PostgreSQL database:**
   ```bash
   docker-compose up -d
   ```

2. **Build and run the application:**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

3. **Verify the application is running:**
   - Application starts on `http://localhost:8080`
   - Database migrations run automatically via Flyway
   - Sample data is initialized automatically (100 documents, 20 authors, 15 tags)

## API Endpoints

### 1. Naive Implementation (N+1 Problem)

```
GET /documents?page=0&size=20
```

**Purpose:** Demonstrates the N+1 query problem.

**Issues:**
- Returns full entity objects (unnecessary data)
- Uses default lazy loading (triggers separate queries for each relationship)
- No explicit fetch strategy
- Large payload size

**Expected Behavior:**
- 1 query to fetch documents
- N queries to fetch authors (N+1 problem)
- N queries to fetch tags (if accessed)
- N queries to fetch permissions (if accessed)

**For 100 documents:** ~1 + 100 + 100 + 100 = **301 queries** (worst case)

### 2. Optimized Implementation

```
GET /documents/optimized?page=0&size=20
```

**Purpose:** Demonstrates optimized query patterns.

**Optimizations Applied:**
1. **DTO Projection:** Only fetches required fields (reduces payload size by ~70%)
2. **JOIN FETCH:** Eagerly fetches author in the same query (eliminates N+1)
3. **Pagination:** Limits result set size
4. **Caching:** Optional Caffeine cache layer for frequently accessed data

**Expected Behavior:**
- **1 query total** with JOIN FETCH to get all data
- Minimal payload (only DTO fields)
- Fast response time

**For 100 documents (page size 20):** **1 query total**

### 3. Document Detail

```
GET /documents/{id}
```

**Purpose:** Fetch single document with all relationships.

**Optimizations:**
- Uses Entity Graph to fetch all relationships in a single query
- Returns DTO with only necessary fields

## Performance Analysis

### Baseline Behavior (Naive Endpoint)

When calling `GET /documents` with 20 documents:

```
=== Query Metrics for GET /documents (naive) ===
Total Queries Executed: 21
Entities Loaded: 21
Collections Loaded: 0
Max Query Time: 15 ms
=====================================
```

**Analysis:**
- 1 query to fetch 20 documents
- 20 additional queries to fetch authors (N+1 problem)
- Total: 21 queries for 20 documents

### Optimized Behavior

When calling `GET /documents/optimized` with 20 documents:

```
=== Query Metrics for GET /documents/optimized ===
Total Queries Executed: 1
Entities Loaded: 20
Collections Loaded: 0
Max Query Time: 8 ms
=====================================
```

**Analysis:**
- 1 query with JOIN FETCH to get all documents and authors
- Total: 1 query for 20 documents
- **95% reduction in query count**

### Performance Improvements

| Metric | Naive | Optimized | Improvement |
|--------|-------|-----------|-------------|
| Queries per request (20 docs) | 21 | 1 | **95% reduction** |
| Response time (avg) | ~150ms | ~50ms | **67% faster** |
| Payload size | ~50KB | ~15KB | **70% smaller** |
| Database load | High | Low | **Significant reduction** |

## Key Optimizations Explained

### 1. DTO Projection

**Problem:** Returning full entity objects includes unnecessary data and triggers lazy loading.

**Solution:** Use constructor-based DTO projection to fetch only required fields:

```java
@Query("""
    SELECT new dto.com.kaczmarek.documentservice.DocumentSummaryDTO(
        d.id, d.title, d.createdAt, a.name, a.id
    )
    FROM Document d
    JOIN FETCH d.author a
    """)
Page<DocumentSummaryDTO> findAllSummaries(Pageable pageable);
```

**Benefits:**
- Reduces payload size
- Prevents lazy loading issues
- Better performance

### 2. JOIN FETCH / Entity Graph

**Problem:** Lazy loading causes N+1 queries when accessing relationships.

**Solution:** Use JOIN FETCH or Entity Graph to eagerly fetch relationships:

```java
@org.springframework.data.jpa.repository.EntityGraph(
    attributePaths = {"author", "tags", "permissions"},
    type = EntityGraphType.FETCH
)
Optional<Document> findByIdWithRelations(Long id);
```

**Benefits:**
- Fetches all related data in a single query
- Eliminates N+1 problem
- Predictable query count

### 3. Pagination

**Problem:** Loading all records at once causes memory issues and slow responses.

**Solution:** Always use pagination:

```java
Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
Page<DocumentSummaryDTO> summaries = repository.findAllSummaries(pageable);
```

**Benefits:**
- Limits result set size
- Better memory usage
- Faster response times

### 4. Database Indexes

**Problem:** Queries without indexes can be slow on large datasets.

**Solution:** Create indexes on frequently queried columns:

```sql
CREATE INDEX idx_document_created_at ON documents(created_at DESC);
CREATE INDEX idx_document_author ON documents(author_id);
```

**Benefits:**
- Faster query execution
- Better join performance

### 5. Caching (Optional)

**Problem:** Repeated queries for the same data waste database resources.

**Solution:** Use Caffeine cache for frequently accessed data:

```java
@Cacheable(value = "documents", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
public Page<DocumentSummaryDTO> findAllOptimized(Pageable pageable) {
    // ...
}
```

**Benefits:**
- Reduces database load
- Faster response for cached data
- Better scalability

## Monitoring and Logging

The application includes comprehensive logging and metrics:

- **SQL Logging:** All SQL queries are logged (see `application.properties`)
- **Hibernate Statistics:** Query counts and execution times are tracked
- **Query Metrics:** Each endpoint logs query statistics

Check the application logs to see:
- Number of queries executed per request
- Query execution times
- Entity and collection load counts

## Database Schema

The database schema is managed by Flyway migrations:

- `V1__Create_tables.sql` - Creates all tables and indexes

Key indexes:
- `idx_document_created_at` - For sorting by creation date
- `idx_document_author` - For author joins
- `idx_permission_document` - For permission lookups
- `idx_document_tags_*` - For tag relationships

## Configuration

Key configuration options in `application.properties`:

```properties
# Enable SQL logging
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Enable Hibernate statistics
spring.jpa.properties.hibernate.generate_statistics=true

# Data initialization
app.data.initialize=true
app.data.documents.count=100
app.data.authors.count=20
app.data.tags.count=15
```

## Testing the Performance Difference

1. **Start the application** and wait for data initialization

2. **Test naive endpoint:**
   ```bash
   curl "http://localhost:8080/documents?page=0&size=20"
   ```
   Check logs for query count (should be ~21 queries)

3. **Test optimized endpoint:**
   ```bash
   curl "http://localhost:8080/documents/optimized?page=0&size=20"
   ```
   Check logs for query count (should be 1 query)

4. **Compare response times and payload sizes**

## Lessons Learned

1. **Always use DTOs for API responses** - Never return entities directly
2. **Use JOIN FETCH or Entity Graph** - Avoid lazy loading in read operations
3. **Implement pagination** - Never load all records at once
4. **Add database indexes** - Index foreign keys and frequently queried columns
5. **Monitor query counts** - Use Hibernate statistics to identify N+1 problems
6. **Consider caching** - For frequently accessed, relatively static data

## Project Structure

```
src/main/java/com/example/documentservice/
├── config/
│   └── DataInitializer.java          # Sample data initialization
├── controller/
│   └── DocumentController.java       # REST endpoints
├── domain/
│   ├── Author.java                   # Author entity
│   ├── Document.java                 # Document entity
│   ├── Permission.java               # Permission entity
│   └── Tag.java                      # Tag entity
├── dto/
│   ├── DocumentDetailDTO.java        # Detail DTO
│   ├── DocumentSummaryDTO.java       # Summary DTO
│   └── QueryMetricsDTO.java          # Metrics DTO
├── repository/
│   ├── AuthorRepository.java
│   ├── DocumentRepository.java       # Custom query methods
│   ├── PermissionRepository.java
│   └── TagRepository.java
├── service/
│   └── DocumentService.java          # Business logic
└── util/
    └── QueryMetricsCollector.java    # Query statistics

src/main/resources/
├── application.properties            # Configuration
└── db/migration/
    └── V1__Create_tables.sql         # Flyway migration
```

## License

See LICENSE file for details.


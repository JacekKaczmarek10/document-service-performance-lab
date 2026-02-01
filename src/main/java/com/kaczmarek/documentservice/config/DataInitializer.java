package com.kaczmarek.documentservice.config;

import com.kaczmarek.documentservice.domain.Author;
import com.kaczmarek.documentservice.domain.Document;
import com.kaczmarek.documentservice.domain.Permission;
import com.kaczmarek.documentservice.domain.Tag;
import com.kaczmarek.documentservice.repository.AuthorRepository;
import com.kaczmarek.documentservice.repository.DocumentRepository;
import com.kaczmarek.documentservice.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Initializes sample data for performance testing.
 * Creates enough data to clearly demonstrate N+1 query problems.
 * 
 * Configured via application.properties:
 * - app.data.initialize=true/false
 * - app.data.documents.count=100
 * - app.data.authors.count=20
 * - app.data.tags.count=15
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.data.initialize", havingValue = "true", matchIfMissing = false)
public class DataInitializer implements CommandLineRunner {
    
    private final AuthorRepository authorRepository;
    private final TagRepository tagRepository;
    private final DocumentRepository documentRepository;
    
    private static final String[] SAMPLE_TITLES = {
        "Introduction to Spring Boot",
        "Advanced Hibernate Performance",
        "Database Optimization Techniques",
        "RESTful API Design Patterns",
        "Microservices Architecture",
        "Cloud Native Applications",
        "Container Orchestration Guide",
        "Event-Driven Systems",
        "Domain-Driven Design",
        "Clean Code Principles",
        "Test-Driven Development",
        "Continuous Integration",
        "DevOps Best Practices",
        "Security in Modern Applications",
        "Scalability Patterns"
    };
    
    private static final String[] TAG_NAMES = {
        "Java", "Spring", "Hibernate", "Performance", "Database",
        "API", "Microservices", "Cloud", "DevOps", "Testing",
        "Architecture", "Design", "Security", "Scalability", "Best-Practices"
    };
    
    private static final Permission.PermissionType[] PERMISSION_TYPES = 
        Permission.PermissionType.values();
    
    @Override
    public void run(String... args) {
        log.info("Starting data initialization...");
        
        // Check if data already exists
        if (documentRepository.count() > 0) {
            log.info("Data already exists. Skipping initialization.");
            return;
        }
        
        // Create authors
        List<Author> authors = createAuthors();
        log.info("Created {} authors", authors.size());
        
        // Create tags
        List<Tag> tags = createTags();
        log.info("Created {} tags", tags.size());
        
        // Create documents with relationships
        createDocuments(authors, tags);
        log.info("Data initialization completed successfully!");
    }
    
    private List<Author> createAuthors() {
        List<Author> authors = new ArrayList<>();
        int authorCount = 20; // Default, can be configured
        
        for (int i = 1; i <= authorCount; i++) {
            Author author = Author.builder()
                .name("Author " + i)
                .build();
            authors.add(authorRepository.save(author));
        }
        
        return authors;
    }
    
    private List<Tag> createTags() {
        List<Tag> tags = new ArrayList<>();
        
        for (String tagName : TAG_NAMES) {
            Tag tag = Tag.builder()
                .name(tagName)
                .build();
            tags.add(tagRepository.save(tag));
        }
        
        return tags;
    }
    
    private void createDocuments(List<Author> authors, List<Tag> tags) {
        Random random = new Random();
        int documentCount = 100; // Default, can be configured
        
        for (int i = 1; i <= documentCount; i++) {
            // Select random author
            Author author = authors.get(random.nextInt(authors.size()));
            
            // Create document
            Document document = Document.builder()
                .title(SAMPLE_TITLES[i % SAMPLE_TITLES.length] + " - Part " + i)
                .createdAt(LocalDateTime.now().minusDays(random.nextInt(365)))
                .author(author)
                .build();
            
            // Add random tags (2-5 tags per document)
            int tagCount = 2 + random.nextInt(4);
            for (int j = 0; j < tagCount; j++) {
                Tag tag = tags.get(random.nextInt(tags.size()));
                document.addTag(tag);
            }
            
            // Add random permissions (1-3 permissions per document)
            int permissionCount = 1 + random.nextInt(3);
            for (int j = 0; j < permissionCount; j++) {
                Permission permission = Permission.builder()
                    .type(PERMISSION_TYPES[random.nextInt(PERMISSION_TYPES.length)])
                    .build();
                document.addPermission(permission);
            }
            
            documentRepository.save(document);
            
            if (i % 10 == 0) {
                log.debug("Created {} documents...", i);
            }
        }
    }
}


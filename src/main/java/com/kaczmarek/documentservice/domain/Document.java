package com.kaczmarek.documentservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "documents", indexes = {
    @Index(name = "idx_document_created_at", columnList = "createdAt"),
    @Index(name = "idx_document_author", columnList = "author_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 500)
    private String title;
    
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // ManyToOne relationship with Author
    // Using LAZY by default to demonstrate N+1 problem
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private Author author;
    
    // ManyToMany relationship with Tags
    // Using LAZY by default
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "document_tags",
        joinColumns = @JoinColumn(name = "document_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();
    
    // OneToMany relationship with Permissions
    // Using LAZY by default
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Permission> permissions = new ArrayList<>();
    
    // Helper methods for bidirectional relationships
    public void addTag(Tag tag) {
        tags.add(tag);
        tag.getDocuments().add(this);
    }
    
    public void removeTag(Tag tag) {
        tags.remove(tag);
        tag.getDocuments().remove(this);
    }
    
    public void addPermission(Permission permission) {
        permissions.add(permission);
        permission.setDocument(this);
    }
}


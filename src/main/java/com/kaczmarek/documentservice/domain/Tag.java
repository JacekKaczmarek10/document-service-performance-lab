package com.kaczmarek.documentservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    
    // ManyToMany relationship with Document
    @ManyToMany(mappedBy = "tags")
    @Builder.Default
    private Set<Document> documents = new HashSet<>();
}


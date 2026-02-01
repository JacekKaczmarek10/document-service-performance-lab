package com.kaczmarek.documentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class DocumentServiceApplication {
    
    static void main(String[] args) {
        SpringApplication.run(DocumentServiceApplication.class, args);
    }
}


package com.shorten.url.shortenUrl.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "shortened_urls", indexes = {
    @Index(name = "idx_hash_code", columnList = "hashCode")
})
@Getter
@Setter
@NoArgsConstructor
public class ShortenedUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String hashCode;

    @Column(nullable = false, length = 2048)
    private String originalUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public ShortenedUrl(String hashCode, String originalUrl) {
        this.hashCode = hashCode;
        this.originalUrl = originalUrl;
    }
}

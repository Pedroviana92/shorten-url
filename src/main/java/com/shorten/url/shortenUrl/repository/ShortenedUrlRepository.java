package com.shorten.url.shortenUrl.repository;

import com.shorten.url.shortenUrl.domain.ShortenedUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShortenedUrlRepository extends JpaRepository<ShortenedUrl, Long> {

    // Find by hashCode - Spring generates: SELECT * FROM shortened_urls WHERE hash_code = ?
    Optional<ShortenedUrl> findByHashCode(String hashCode);

    // Check if hashCode exists - Spring generates: SELECT COUNT(*) > 0 FROM shortened_urls WHERE hash_code = ?
    boolean existsByHashCode(String hashCode);

    // Find by original URL - Spring generates: SELECT * FROM shortened_urls WHERE original_url = ?
    Optional<ShortenedUrl> findByOriginalUrl(String originalUrl);
}

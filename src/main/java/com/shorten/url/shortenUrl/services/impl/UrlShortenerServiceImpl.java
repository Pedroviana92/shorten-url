package com.shorten.url.shortenUrl.services.impl;

import com.shorten.url.shortenUrl.domain.ShortenedUrl;
import com.shorten.url.shortenUrl.repository.ShortenedUrlRepository;
import com.shorten.url.shortenUrl.responseHandler.ResponseJson;
import com.shorten.url.shortenUrl.services.UrlShortenerService;
import org.apache.commons.validator.routines.UrlValidator;
import org.hashids.Hashids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Transactional
public class UrlShortenerServiceImpl implements UrlShortenerService {

    // REMOVED: private final Map<String, String> urlMap = new HashMap<>();
    // NEW: Inject the repository for database operations
    private final ShortenedUrlRepository repository;

    private final AtomicLong idGenerator = new AtomicLong(1000);
    private final UrlValidator urlValidator = new UrlValidator(new String[]{"http", "https"});
    private Hashids hashids;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.hashids.salt}")
    private String hashidsSalt;

    @Value("${app.hashids.min-length}")
    private int hashidsMinLength;

    // NEW: Constructor injection of repository
    @Autowired
    public UrlShortenerServiceImpl(ShortenedUrlRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void init() {
        this.hashids = new Hashids(hashidsSalt, hashidsMinLength);
    }

    @Override
    public ResponseJson shortenUrl(String originalUrl) {
        // Validate URL format
        if (originalUrl == null || originalUrl.trim().isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "URL cannot be empty. Please enter a valid URL."
            );
        }

        if (!urlValidator.isValid(originalUrl)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid URL format. Please enter a valid URL starting with http:// or https://"
            );
        }

        // Generate unique hashCode (ensure no collision)
        String hashCode;
        do {
            long uniqueId = idGenerator.incrementAndGet();
            hashCode = hashids.encode(uniqueId);
        } while (repository.existsByHashCode(hashCode));

        String shortenUrl = baseUrl + hashCode;

        // CHANGED: Save to database instead of HashMap
        // OLD: urlMap.put(hashCode, originalUrl);
        ShortenedUrl shortenedUrl = new ShortenedUrl(hashCode, originalUrl);
        repository.save(shortenedUrl);

        return new ResponseJson(shortenUrl, originalUrl, "Url shortened successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public String getOriginalUrl(String hashCode) {
        // CHANGED: Query database instead of HashMap
        // OLD: String originalUrl = urlMap.get(hashCode);
        return repository.findByHashCode(hashCode)
            .map(ShortenedUrl::getOriginalUrl)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Shortened URL not found"
            ));
    }
}

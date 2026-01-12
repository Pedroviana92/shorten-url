package com.shorten.url.shortenUrl.services.impl;

import com.shorten.url.shortenUrl.domain.ShortenedUrl;
import com.shorten.url.shortenUrl.repository.ShortenedUrlRepository;
import com.shorten.url.shortenUrl.responseHandler.ResponseJson;
import com.shorten.url.shortenUrl.services.UrlShortenerService;
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

    private final ShortenedUrlRepository repository;
    private final AtomicLong idGenerator = new AtomicLong(1000);
    private Hashids hashids;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.hashids.salt}")
    private String hashidsSalt;

    @Value("${app.hashids.min-length}")
    private int hashidsMinLength;

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
        // Validation is now handled by @Valid in the controller layer

        // Generate unique hashCode (ensure no collision)
        String hashCode;
        do {
            long uniqueId = idGenerator.incrementAndGet();
            hashCode = hashids.encode(uniqueId);
        } while (repository.existsByHashCode(hashCode));

        String shortenUrl = baseUrl + hashCode;

        // Save to database
        ShortenedUrl shortenedUrl = new ShortenedUrl(hashCode, originalUrl);
        repository.save(shortenedUrl);

        return new ResponseJson(shortenUrl, originalUrl, "Url shortened successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public String getOriginalUrl(String hashCode) {
        return repository.findByHashCode(hashCode)
            .map(ShortenedUrl::getOriginalUrl)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Shortened URL not found"
            ));
    }
}

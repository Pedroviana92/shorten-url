package com.shorten.url.shortenUrl.services.impl;

import com.shorten.url.shortenUrl.responseHandler.ResponseJson;
import com.shorten.url.shortenUrl.services.UrlShortenerService;
import org.apache.commons.validator.routines.UrlValidator;
import org.hashids.Hashids;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UrlShortenerServiceImpl implements UrlShortenerService {

    private final Map<String, String> urlMap = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1000);
    private final UrlValidator urlValidator = new UrlValidator(new String[]{"http", "https"});
    private Hashids hashids;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.hashids.salt}")
    private String hashidsSalt;

    @Value("${app.hashids.min-length}")
    private int hashidsMinLength;

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

        long uniqueId = idGenerator.incrementAndGet();
        String hashCode = hashids.encode(uniqueId);
        String shortenUrl = baseUrl + hashCode;

        urlMap.put(hashCode, originalUrl);

        return new ResponseJson(shortenUrl, originalUrl, "Url shortened successfully");
    }

    @Override
    public String getOriginalUrl(String hashCode) {
        String originalUrl = urlMap.get(hashCode);

        if (originalUrl == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Shortened URL not found");
        }

        return originalUrl;
    }
}

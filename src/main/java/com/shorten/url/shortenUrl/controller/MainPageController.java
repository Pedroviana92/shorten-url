package com.shorten.url.shortenUrl.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import com.shorten.url.shortenUrl.responseHandler.ResponseJson;
import com.shorten.url.shortenUrl.responseHandler.UrlRequest;
import com.shorten.url.shortenUrl.services.CacheService;
import com.shorten.url.shortenUrl.services.UrlShortenerService;

@RestController
@RequestMapping("/")
public class MainPageController {

    private final UrlShortenerService urlShortenerService;
    private final CacheService cacheService;

    @Autowired
    public MainPageController(UrlShortenerService urlShortenerService, CacheService cacheService) {
        this.urlShortenerService = urlShortenerService;
        this.cacheService = cacheService;
    }

    @PostMapping("/shorten")
    public ResponseJson shortenUrl(
            @Valid @RequestBody UrlRequest urlRequest,
            HttpServletRequest httpRequest) {
        try {
            // Generate fingerprint for idempotency
            String fingerprint = generateFingerprint(urlRequest.getUrl(), httpRequest);
            String cacheKey = "idempotency:" + fingerprint;

            // Use cache for idempotency - returns cached response if duplicate request
            return cacheService.getOrCompute(
                cacheKey,
                () -> urlShortenerService.shortenUrl(urlRequest.getUrl()),
                ResponseJson.class
            );

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An error occurred while shortening the URL. Please try again."
            );
        }
    }

    @GetMapping("/{hashCode}")
    public ResponseEntity<Void> redirectToOriginalUrl(@PathVariable String hashCode) {
        try {
            String originalUrl = urlShortenerService.getOriginalUrl(hashCode);

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(originalUrl));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);

        } catch (ResponseStatusException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                HttpHeaders headers = new HttpHeaders();
                headers.setLocation(URI.create("/errors/not-found.html"));
                return new ResponseEntity<>(headers, HttpStatus.FOUND);
            }
            throw e;

        } catch (IllegalArgumentException e) {
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create("/errors/invalid-url.html"));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);

        } catch (Exception e) {
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create("/errors/server-error.html"));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }
    }

    /**
     * Generates a unique fingerprint based on URL and client identifier.
     * Same URL from same client = same fingerprint (for idempotency)
     * Same URL from different client = different fingerprint (allows duplicates)
     */
    private String generateFingerprint(String url, HttpServletRequest request) {
        String clientId = extractClientIdentifier(request);
        String raw = url + "|" + clientId;
        return hashSha256(raw);
    }

    /**
     * Extracts a client identifier from the request.
     * Priority: X-Forwarded-For > Remote Address > Session ID
     */
    private String extractClientIdentifier(HttpServletRequest request) {
        // Check for proxy header first
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        // Use remote address
        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr != null && !remoteAddr.isBlank()) {
            return remoteAddr;
        }

        // Fallback to session
        return request.getSession(true).getId();
    }

    /**
     * Creates a SHA-256 hash of the input string.
     */
    private String hashSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}

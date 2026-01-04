package com.shorten.url.shortenUrl.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.validator.routines.UrlValidator;
import org.hashids.Hashids;
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
import java.net.URI;

import com.shorten.url.shortenUrl.responseHandler.ResponseJson;
import com.shorten.url.shortenUrl.responseHandler.UrlRequest;



@RestController
@RequestMapping("/")
public class MainPageController {


    private final Map<String, String> urlMap = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1000);
    private final Hashids hashids = new Hashids("my-secret-salt-2026", 6);
    private final UrlValidator urlValidator = new UrlValidator(new String[]{"http", "https"});

    @PostMapping("/shorten")
    public ResponseJson shortenUrl(@RequestBody UrlRequest urlRequest) {
        try {
            String url = urlRequest.getUrl();

            // Validate URL format
            if (url == null || url.trim().isEmpty()) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "URL cannot be empty. Please enter a valid URL."
                );
            }

            if (!urlValidator.isValid(url)) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid URL format. Please enter a valid URL starting with http:// or https://"
                );
            }

            long uniqueId = idGenerator.incrementAndGet();
            String hashCode = hashids.encode(uniqueId);
            String shortenUrl = "http://localhost:8080/" + hashCode;

            urlMap.put(hashCode, url);

            System.out.println(urlMap);

            return new ResponseJson(shortenUrl, url, "Url shortened successfully");

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
            String originalUrl = urlMap.get(hashCode);

            if (originalUrl == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Shortened URL not found");
            }

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
}

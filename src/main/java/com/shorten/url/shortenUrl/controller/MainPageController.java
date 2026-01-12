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

import jakarta.validation.Valid;
import java.net.URI;

import com.shorten.url.shortenUrl.responseHandler.ResponseJson;
import com.shorten.url.shortenUrl.responseHandler.UrlRequest;
import com.shorten.url.shortenUrl.services.UrlShortenerService;

@RestController
@RequestMapping("/")
public class MainPageController {

    private final UrlShortenerService urlShortenerService;

    @Autowired
    public MainPageController(UrlShortenerService urlShortenerService) {
        this.urlShortenerService = urlShortenerService;
    }

    @PostMapping("/shorten")
    public ResponseJson shortenUrl(@Valid @RequestBody UrlRequest urlRequest) {
        try {
            return urlShortenerService.shortenUrl(urlRequest.getUrl());
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
}

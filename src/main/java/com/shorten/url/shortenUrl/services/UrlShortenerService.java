package com.shorten.url.shortenUrl.services;

import com.shorten.url.shortenUrl.responseHandler.ResponseJson;

public interface UrlShortenerService {
    ResponseJson shortenUrl(String originalUrl);
    String getOriginalUrl(String hashCode);
}

package com.shorten.url.shortenUrl.responseHandler;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ResponseJson {
    private String shortUrl;
    private String originalUrl;
    private String message;

}

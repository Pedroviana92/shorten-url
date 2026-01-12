package com.shorten.url.shortenUrl.responseHandler;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResponseJson {
    private String shortUrl;
    private String originalUrl;
    private String message;

}

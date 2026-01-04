package com.shorten.url.shortenUrl.services;

import org.springframework.stereotype.Service;

@Service
public class BaseConversion {
    public String convertToBase62String(int number) {
        String base62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        String result = "";

        if (number == 0) {
            return base62.charAt(0) + "";
        }

        while (number > 0) {
            result = base62.charAt(number % 62) + result;
            number /= base62.length();
        }
        return result;
    }
}

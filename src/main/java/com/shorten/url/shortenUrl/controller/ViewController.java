package com.shorten.url.shortenUrl.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/home")
    public String homePage() {
        return "home";
    }
}

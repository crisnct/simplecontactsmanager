package com.example.contacts.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class PageController {

    @GetMapping("/login")
    public String login() {
        log.info("Rendering login page");
        return "login";
    }

    @GetMapping("/signup")
    public String signup() {
        log.info("Rendering signup page");
        return "signup";
    }
}

package com.turkcell.user_service.web.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("")
    public String get() {
        System.out.println("UserController çalıştı");
        return "UserController";
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello User-Service";
    }
}

package com.game.monopoly.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/login")
    public String loginPage() {
        return "redirect:/auth/login.html";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "redirect:/auth/register.html";
    }

    @GetMapping("/home")
    public String homePage() {
        return "redirect:/home/home.html";
    }

    @GetMapping("/admin")
    public String adminPage() {
        return "redirect:/admin/index.html";
    }
}
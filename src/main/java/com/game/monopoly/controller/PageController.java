package com.game.monopoly.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "auth/register";
    }

    @GetMapping("/home")
    public String mainMenu() {
        return "main-menu";
    }

    @GetMapping("/private-table")
    public String privateTable() {
        return "private-table";
    }

    @GetMapping("/game-board")
    public String gameBoard() {
        return "game-board";
    }

    @GetMapping("/map-editor")
    public String mapEditor() {
        return "map-editor";
    }

    @GetMapping("/shop")
    public String shop() {
        return "shop";
    }

    @GetMapping("/admin")
    public String adminPage() {
        return "admin/index";
    }
}

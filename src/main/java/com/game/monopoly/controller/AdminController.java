package com.game.monopoly.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardStats() {
        // Dữ liệu mẫu (có thể lấy từ DB sau)
        return ResponseEntity.ok(Map.of(
            "totalUsers", 150,
            "activeGames", 12,
            "revenue", "5,000,000 VND"
        ));
    }
}

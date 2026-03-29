package com.game.monopoly.controller;

import com.game.monopoly.dto.ChangeDisplayNameRequest;
import com.game.monopoly.dto.SetCurrentHeroRequest;
import com.game.monopoly.dto.UserMeSummaryResponse;
import com.game.monopoly.dto.UserMeAvatarResponse;
import com.game.monopoly.service.UserMeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user/me")
@RequiredArgsConstructor
public class UserMeController {

    private final UserMeService userMeService;

    // Lấy thông tin tổng quan của User
    @GetMapping("/summary")
    public ResponseEntity<UserMeSummaryResponse> getSummary(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId) {

        UserMeSummaryResponse response = userMeService.getSummary(accountId);
        return ResponseEntity.ok(response);
    }

    // Cập nhật tên hiển thị (Đã gộp chung phần logic của đổi Username)
    @PutMapping("/display-name")
    public ResponseEntity<UserMeSummaryResponse> changeDisplayName(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @RequestBody ChangeDisplayNameRequest body) {

        String name = body != null ? body.getNewUsername() : null;
        UserMeSummaryResponse response = userMeService.changeDisplayName(accountId, name);

        return ResponseEntity.ok(response);
    }

    // Cập nhật Avatar
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserMeAvatarResponse> updateAvatar(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @RequestParam("avatar") MultipartFile avatar) {

        String avatarUrl = userMeService.updateAvatar(accountId, avatar);
        return ResponseEntity.ok(UserMeAvatarResponse.builder().avatarUrl(avatarUrl).build());
    }

    // Trang bị nhân vật (Đã gộp chung phần logic của Equip Hero)
    @PutMapping("/current-hero")
    public ResponseEntity<UserMeSummaryResponse> setCurrentHero(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @RequestBody SetCurrentHeroRequest body) {

        Integer heroId = body != null ? body.getHeroId() : null;
        UserMeSummaryResponse response = userMeService.setCurrentHero(accountId, heroId);

        return ResponseEntity.ok(response);
    }
}
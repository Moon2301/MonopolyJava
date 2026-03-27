package com.game.monopoly.controller;

import com.game.monopoly.dto.UserMeSummaryResponse;
import com.game.monopoly.dto.UserMeAvatarResponse;
import com.game.monopoly.service.UserMeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user/me")
@RequiredArgsConstructor
public class UserMeController {

    private final UserMeService userMeService;

    @GetMapping("/summary")
    public UserMeSummaryResponse getSummary(@RequestHeader(name = "X-Account-Id", required = false) Long accountId) {
        return userMeService.getSummary(accountId);
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserMeAvatarResponse updateAvatar(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @RequestParam("avatar") MultipartFile avatar
    ) {
        String avatarUrl = userMeService.updateAvatar(accountId, avatar);
        return UserMeAvatarResponse.builder().avatarUrl(avatarUrl).build();
    }
}


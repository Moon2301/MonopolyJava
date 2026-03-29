package com.game.monopoly.controller;

import com.game.monopoly.dto.FriendRequestDto;
import com.game.monopoly.dto.FriendResponse;
import com.game.monopoly.dto.MessageResponse;
import com.game.monopoly.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @GetMapping
    public FriendResponse getFriends(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId) {
        if (accountId == null) throw new RuntimeException("Khong co ID tai khoan");
        return friendService.getFriends(accountId);
    }

    @GetMapping("/pending-count")
    public java.util.Map<String, Integer> getPendingCount(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId) {
        if (accountId == null) return java.util.Map.of("count", 0);
        return java.util.Map.of("count", friendService.getPendingCount(accountId));
    }

    @PostMapping("/request")
    public MessageResponse sendRequest(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @RequestBody FriendRequestDto request) {
        if (accountId == null) throw new RuntimeException("Khong co ID tai khoan");
        return friendService.sendFriendRequest(request, accountId);
    }

    @PostMapping("/accept/{id}")
    public MessageResponse acceptRequest(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @PathVariable Long id) {
        if (accountId == null) throw new RuntimeException("Khong co ID tai khoan");
        return friendService.acceptFriendRequest(id, accountId);
    }

    @PostMapping("/decline/{id}")
    public MessageResponse declineOrRemove(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @PathVariable Long id) {
        if (accountId == null) throw new RuntimeException("Khong co ID tai khoan");
        return friendService.declineOrRemoveFriend(id, accountId);
    }
}

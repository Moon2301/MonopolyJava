package com.game.monopoly.controller;

import com.game.monopoly.dto.*;
import com.game.monopoly.service.SocialService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/social")
@RequiredArgsConstructor
public class SocialController {

    private final SocialService socialService;

    @GetMapping("/friends")
    public List<FriendListItemResponse> getFriends(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId
    ) {
        return socialService.getFriends(accountId);
    }

    @PostMapping("/friends/request")
    public FriendListItemResponse sendFriendRequest(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @RequestBody FriendRequestDto request
    ) {
        return socialService.sendFriendRequest(accountId, request.getUsername());
    }

    @PostMapping("/friends/{friendId}/accept")
    public FriendListItemResponse acceptFriendRequest(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @PathVariable Long friendId
    ) {
        return socialService.acceptFriendRequest(accountId, friendId);
    }

    @GetMapping("/messages")
    public List<MessageItemResponse> getConversation(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @RequestParam("friendUserProfileId") Long friendUserProfileId
    ) {
        return socialService.getConversation(accountId, friendUserProfileId);
    }

    @PostMapping("/messages")
    public MessageItemResponse sendMessage(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @RequestBody SendMessageRequest request
    ) {
        return socialService.sendMessage(accountId, request.getToUserProfileId(), request.getContent());
    }

    @GetMapping("/channel/messages")
    public List<MessageItemResponse> getChannelMessages(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId
    ) {
        return socialService.getChannelMessages(accountId);
    }

    @PostMapping("/channel/messages")
    public MessageItemResponse sendChannelMessage(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @RequestBody SendMessageRequest request
    ) {
        return socialService.sendChannelMessage(accountId, request.getContent());
    }
}

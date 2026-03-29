package com.game.monopoly.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FriendResponse {
    private List<FriendDto> friends;
    private List<FriendDto> pendingRequests;

    @Data
    @Builder
    public static class FriendDto {
        private Long friendshipId;
        private Long accountId;
        private String username;
        private String avatarUrl;
    }
}

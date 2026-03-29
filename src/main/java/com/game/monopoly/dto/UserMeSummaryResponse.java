package com.game.monopoly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserMeSummaryResponse {

    private String username;
    private String avatarUrl;
    private Long coins;
    private Long tickets;
    private String rankTier;

    private Integer matches;
    private Integer winRate;
    private Long totalWonAssets;

    /** Nhân vật mặc định (hồ sơ / bàn chơi). */
    private Integer equippedCharacterId;
    private String equippedCharacterName;
    private String equippedCharacterImageUrl;
}


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

    // Hero/character user is using (based on latest finished game)
    private Integer equippedCharacterId;
    private String equippedCharacterName;
    private String equippedCharacterImageUrl;
}


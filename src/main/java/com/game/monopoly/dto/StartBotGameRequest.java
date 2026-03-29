package com.game.monopoly.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StartBotGameRequest {
    private String difficulty; // easy | hard
    private Integer heroId;
    /** Số bot (1–3). Mặc định 1. */
    private Integer botCount;
}

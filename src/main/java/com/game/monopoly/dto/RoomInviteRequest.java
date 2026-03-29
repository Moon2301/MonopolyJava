package com.game.monopoly.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.Data;
@Getter
@Setter
public class RoomInviteRequest {
    private Long roomId;
    private Long toUserProfileId;

    private String username;
}

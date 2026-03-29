package com.game.monopoly.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomInviteRequest {
    private Long roomId;
    private Long toUserProfileId;
}

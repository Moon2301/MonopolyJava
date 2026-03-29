package com.game.monopoly.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomInviteRequest {
    private Long roomId;
    private Long toUserProfileId;
import lombok.Data;

@Data
public class RoomInviteRequest {
    private String username;
}

package com.game.monopoly.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class RoomInvitationResponse {
    private Long id;
    private Long roomId;
    private String roomCode;
    private String inviterName;
    private LocalDateTime createdAt;
}

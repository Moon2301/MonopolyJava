package com.game.monopoly.model.metaData;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_invitations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomInvitation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "inviter_id")
    private Long inviterId;

    @Column(name = "invitee_id")
    private Long inviteeId;

    @Column(name = "status")
    private String status; // PENDING

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

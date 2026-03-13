package com.game.monopoly.model.inGameData;

import com.game.monopoly.model.enums.GameStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Game")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_id")
    private Long gameId;

    @Column(name = "map_id")
    private Integer mapId;

    @Column(name = "created_by")
    private Long createdBy;

    @Enumerated(EnumType.STRING)
    private GameStatus status;

    private Integer maxPlayers;
    private Integer currentTurn;
    private Integer currentPlayerOrder;

    private String turnState;

    private Integer version;

    private Long winnerPlayerId;

    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
}
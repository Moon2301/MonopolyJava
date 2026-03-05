package com.game.monopoly.model.inGameData;

import com.game.monopoly.model.enums.GameStatus;
import com.game.monopoly.model.enums.TurnState;
import com.game.monopoly.model.metaData.Map;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Game")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long gameId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "map_id", nullable = false)
    private Map map;

    private Long createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameStatus status = GameStatus.WAITING;

    private Integer maxPlayers = 4;
    private Integer currentTurn = 1;
    private Integer currentPlayerOrder = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TurnState turnState = TurnState.WAIT_ROLL;

    @Version
    private Integer version; // Chống Race Condition

    private Long winnerPlayerId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL)
    private List<GamePlayer> players;
}
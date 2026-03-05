package com.game.monopoly.model.inGameData;

import com.game.monopoly.model.metaData.Hero;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "GamePlayer", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"game_id", "turn_order"}),
        @UniqueConstraint(columnNames = {"game_id", "user_profile_id"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class GamePlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long gamePlayerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "user_profile_id", nullable = false)
    private Long userProfileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private Hero hero;

    private Integer turnOrder;
    private Long balance = 0L;
    private Integer position = 0;
    private Boolean isBankrupt = false;
    private Boolean isBot = false;
}
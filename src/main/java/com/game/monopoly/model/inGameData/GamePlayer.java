package com.game.monopoly.model.inGameData;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "GamePlayer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GamePlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_player_id")
    private Long gamePlayerId;

    @Column(name = "game_id")
    private Long gameId;

    @Column(name = "user_profile_id")
    private Long userProfileId;

    @Column(name = "character_id")
    private Integer characterId;

    private Integer turnOrder;
    private Long balance;
    private Integer position;

    private Boolean isBankrupt;
    private Boolean isBot;
}
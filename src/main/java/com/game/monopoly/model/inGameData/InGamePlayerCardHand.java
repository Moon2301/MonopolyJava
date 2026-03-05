package com.game.monopoly.model.inGameData;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Entity
@Table(name = "InGamePlayerCardHand")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class InGamePlayerCardHand {

    @EmbeddedId
    private PlayerCardHandId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("gamePlayerId")
    @JoinColumn(name = "game_player_id")
    private GamePlayer gamePlayer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    private Integer acquiredTurn;
}

@Embeddable
@Data
@NoArgsConstructor @AllArgsConstructor
class PlayerCardHandId implements Serializable {
    private Long gamePlayerId;
    private Integer cardId;
    private Integer acquiredTurn;
}
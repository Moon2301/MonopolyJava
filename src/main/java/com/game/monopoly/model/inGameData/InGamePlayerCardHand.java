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
    @MapsId("gamePlayerId") // Ánh xạ vào gamePlayerId trong PlayerCardHandId
    @JoinColumn(name = "game_player_id")
    private GamePlayer gamePlayer;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("cardId") // Ánh xạ vào cardId trong PlayerCardHandId
    @JoinColumn(name = "card_id")
    private Card card;
}

@Embeddable
@Data
@NoArgsConstructor @AllArgsConstructor
class PlayerCardHandId implements Serializable {
    private Long gamePlayerId;
    private Integer cardId;
    private Integer acquiredTurn;
}
package com.game.monopoly.model.inGameData;

import com.game.monopoly.model.enums.ZoneType;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "GameDeck")
@Getter @Setter
@NoArgsConstructor
public class GameDeck {

    @EmbeddedId
    private GameDeckId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("gameId") // Ánh xạ trường gameId trong GameDeckId
    @JoinColumn(name = "game_id")
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

}

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
class GameDeckId implements Serializable {

    private Long gameId;

    @Enumerated(EnumType.STRING)
    private ZoneType zone;

    private Integer position;
}

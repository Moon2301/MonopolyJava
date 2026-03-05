package com.game.monopoly.model.inGameData;

import com.game.monopoly.model.metaData.BoardCell;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "PlayerProperty")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class PlayerProperty {

    @EmbeddedId
    private PlayerPropertyId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("gameId")
    @JoinColumn(name = "game_id")
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("cellId")
    @JoinColumn(name = "cell_id")
    private BoardCell boardCell;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_player_id")
    private GamePlayer owner;

    private Integer houseLevel = 0;
}

@Embeddable
@Data
class PlayerPropertyId implements Serializable {
    private Long gameId;
    private Integer cellId;
}
package com.game.monopoly.model.metaData;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "MapCell")
@Getter
@Setter
@NoArgsConstructor
public class MapCell {

    @EmbeddedId
    private MapCellId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("mapId") // Ánh xạ mapId từ MapCellId vào entity Map
    @JoinColumn(name = "map_id")
    private Map map;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cell_id", nullable = false)
    private BoardCell boardCell;
}

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
class MapCellId implements Serializable {
    private Integer mapId;
    private Integer position;
}

package com.game.monopoly.model.metaData;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "Map")
@Getter @Setter
public class Map {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer mapId;

    private String name;
    private Integer maxLaps = 20;
    private Boolean isActive = true;

    @OneToMany(mappedBy = "map", cascade = CascadeType.ALL)
    @OrderBy("id.position ASC") // Sắp xếp theo vị trí tăng dần trên bàn cờ
    private List<MapCell> mapCells;
}


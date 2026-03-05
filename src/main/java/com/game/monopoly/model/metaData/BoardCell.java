package com.game.monopoly.model.metaData;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "BoardCell")
@Getter @Setter
public class BoardCell {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer cellId;

    private String name;
    private String type; // PRISON, START, PROPERTY, CHANCE...
    private Integer price;
    private Integer baseRent;
    private Integer maxHouseLevel;
}

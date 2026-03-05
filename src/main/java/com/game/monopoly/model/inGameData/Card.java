package com.game.monopoly.model.inGameData;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Card")
@Getter @Setter
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer cardId;

    private String name;
    private String effectType;
    private Integer effectValue;

    @Column(columnDefinition = "JSON") // Lưu ý: Cần config Jackson để map JSON
    private String effectData;

    private String rarity;
}

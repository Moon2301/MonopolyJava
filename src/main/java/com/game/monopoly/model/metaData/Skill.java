package com.game.monopoly.model.metaData;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Skill")
@Getter @Setter
public class Skill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer skillId;

    private String name;
    private String triggerType;
    private String effectType;
    private Integer cooldown;
    private Integer effectValue;
    private String effectFormula;
}

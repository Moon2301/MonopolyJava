package com.game.monopoly.model.metaData;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "Hero")
@Getter @Setter
public class Hero {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer characterId;

    @Column(unique = true, nullable = false)
    private String name;
    private String rarity;
    private Integer baseHp;
    private Integer baseIncomeBonus;
    private Boolean isActive = true;

    @ManyToMany
    @JoinTable(
            name = "CharacterSkill",
            joinColumns = @JoinColumn(name = "character_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private List<Skill> skills;
}


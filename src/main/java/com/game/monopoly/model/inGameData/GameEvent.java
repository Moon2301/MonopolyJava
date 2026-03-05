package com.game.monopoly.model.inGameData;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "GameEvent")
@Getter @Setter
public class GameEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eventId;

    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;

    private Integer turnNumber;

    @ManyToOne
    @JoinColumn(name = "actor_player_id")
    private GamePlayer actor;

    private String eventType;

    @Column(columnDefinition = "JSON")
    private String payload;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
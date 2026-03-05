package com.game.monopoly.model.metaData;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "UserProfile")
@Getter @Setter
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userProfileId;

    @OneToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(unique = true, nullable = false)
    private String username;

    private Long gold = 0L;
    private Long diamonds = 0L;
    private Integer rankPoints = 1000;
    private String rankTier = "BRONZE";
}
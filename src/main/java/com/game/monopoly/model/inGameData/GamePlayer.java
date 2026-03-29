package com.game.monopoly.model.inGameData;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "game_player") // ✅ nên dùng snake_case cho chuẩn DB
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GamePlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_player_id")
    private Long gamePlayerId;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(name = "user_profile_id", nullable = false)
    private Long userProfileId;

    @Column(name = "character_id")
    private Integer characterId;

    // 🔥 FIX QUAN TRỌNG (lỗi của bạn)
    @Column(name = "turn_order", nullable = false)
    private Integer turnOrder;

    @Column(name = "balance")
    private Long balance;

    @Column(name = "position")
    private Integer position;

    @Column(name = "is_bankrupt")
    private Boolean isBankrupt;

    @Column(name = "is_bot")
    private Boolean isBot;

    /** Đang ở tù */
    @Builder.Default
    @Column(name = "in_jail")
    private Boolean inJail = false;

    /** Số lần lắc fail trong tù */
    @Builder.Default
    @Column(name = "jail_failed_rolls")
    private Integer jailFailedRolls = 0;

    /** Số lần double liên tiếp */
    @Builder.Default
    @Column(name = "consecutive_doubles")
    private Integer consecutiveDoubles = 0;

    /** Cooldown skill */
    @Builder.Default
    @Column(name = "skill_cooldown_remaining")
    private Integer skillCooldownRemaining = 0;
}
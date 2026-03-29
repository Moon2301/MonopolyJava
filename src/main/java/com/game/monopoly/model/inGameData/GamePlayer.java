package com.game.monopoly.model.inGameData;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "GamePlayer")
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

    @Column(name = "game_id")
    private Long gameId;

    @Column(name = "user_profile_id")
    private Long userProfileId;

    @Column(name = "character_id")
    private Integer characterId;

    private Integer turnOrder;
    private Long balance;
    private Integer position;

    private Boolean isBankrupt;
    private Boolean isBot;

    /** Đang ở tù (không đi được cho đến khi đủ điều kiện ra). */
    @Builder.Default
    @Column(name = "in_jail")
    private Boolean inJail = false;

    /**
     * Số lần lắc trong tù không ra đôi. Đủ 3 → lượt sau có thể ra tù theo rule (bất kỳ tổng xx).
     */
    @Builder.Default
    @Column(name = "jail_failed_rolls")
    private Integer jailFailedRolls = 0;

    /** Chuỗi xx liên tiếp trong cùng lượt; lần thứ 3 → vào tù. */
    @Builder.Default
    @Column(name = "consecutive_doubles")
    private Integer consecutiveDoubles = 0;

    /** Hồi chiêu skill (lượt còn lại), 0 = sẵn sàng. */
    @Builder.Default
    @Column(name = "skill_cooldown_remaining")
    private Integer skillCooldownRemaining = 0;
}
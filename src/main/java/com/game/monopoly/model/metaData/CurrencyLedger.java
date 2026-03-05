package com.game.monopoly.model.metaData;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "CurrencyLedger")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CurrencyLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ledgerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @Column(nullable = false)
    private String currencyType; // GOLD hoặc DIAMOND

    @Column(nullable = false)
    private Long amount; // Số lượng thay đổi (vd: +500, -200)

    @Column(nullable = false)
    private Long balanceAfter; // Số dư sau khi thực hiện giao dịch

    @Column(nullable = false)
    private String reasonType; // Lý do: "MATCH_REWARD", "BUY_HERO", "RENT_PAYMENT"...

    private Long referenceId; // ID tham chiếu (vd: gameId hoặc transactionId)

    @CreationTimestamp
    private LocalDateTime createdAt;
}
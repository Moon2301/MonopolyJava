package com.game.monopoly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class GameStateResponse {
    private Long gameId;
    private String status;
    private Integer currentTurn;
    private Integer currentPlayerOrder;
    private String turnState;
    private Integer lastDice1;
    private Integer lastDice2;
    /** Giây còn lại của lượt người (null nếu không phải lượt người hoặc game kết thúc). */
    private Integer turnSecondsRemaining;
    /** Có phải lượt của tài khoản gửi X-Account-Id không (null nếu không gửi header). */
    private Boolean myTurn;
    private Integer totalBoardCells;
    private CellInfoDto currentCell;
    private List<PlayerStateDto> players;
    /** Ô đã có chủ: chỉ số ô trên bàn (0..n-1) + lượt người chơi (1..4). */
    private List<OwnedCellDto> ownedCells;
    /**
     * Thông báo thuê đất vừa xảy ra (chỉ gửi một lần rồi xóa khỏi hàng đợi khi client gọi getState).
     */
    private List<RentNoticeDto> rentNotices;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class RentNoticeDto {
        private String payerName;
        private long amountPaid;
        private String cellName;
        private String ownerName;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class OwnedCellDto {
        private Integer boardIndex;
        private Integer ownerTurnOrder;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class CellInfoDto {
        private Integer cellId;
        private String name;
        private String type;
        private Long price;
        private Long upgradeCost;
        private Long estimatedRent;
        private Long ownerGamePlayerId;
        private Integer ownerTurnOrder;
        private Integer houseLevel;
        private Boolean canBuy;
        private Boolean canUpgrade;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PlayerStateDto {
        private Long gamePlayerId;
        private Long userProfileId;
        private Integer turnOrder;
        private Integer position;
        private Long balance;
        private Boolean isBot;
        private Boolean isBankrupt;
        /** Tên hiển thị (username hoặc Bot). */
        private String username;
        /** Ảnh đại diện tài khoản — dùng sảnh / thẻ người chơi. */
        private String avatarUrl;
        /** Ảnh nhân vật (hero) — ưu tiên hiển thị trên quân cờ bàn. */
        private String heroImageUrl;
        /** Tên hero — dùng với HeroSystem SVG khi không có ảnh. */
        private String heroName;
    }
}

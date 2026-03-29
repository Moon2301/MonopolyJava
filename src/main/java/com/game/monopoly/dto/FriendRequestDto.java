package com.game.monopoly.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;
import lombok.Data;

@Data
public class FriendRequestDto {
    /** Username người muốn kết bạn — JSON có thể dùng {@code friendUsername} (tương thích cũ). */
    @JsonAlias("friendUsername")
    private String username;
}

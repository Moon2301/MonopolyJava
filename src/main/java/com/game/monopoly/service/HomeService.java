package com.game.monopoly.service;

import com.game.monopoly.dto.HomeSummaryResponse;
import com.game.monopoly.model.metaData.Account;
import com.game.monopoly.model.metaData.UserProfile;
import com.game.monopoly.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class HomeService {

    private static final String DEFAULT_AVATAR = "/images/avatar-default.png";
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final UserProfileRepository userProfileRepository;

    public HomeSummaryResponse getHomeSummary(Authentication authentication) {
        Account account = extractAccount(authentication);
        UserProfile profile = userProfileRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new RuntimeException("UserProfile not found"));

        return HomeSummaryResponse.builder()
                .player(HomeSummaryResponse.PlayerDto.builder()
                        .username(profile.getUsername())
                        .avatarUrl(DEFAULT_AVATAR)
                        .coins(profile.getGold())
                        .tickets(profile.getDiamonds())
                        .build())
                .onlineStats(HomeSummaryResponse.OnlineStatsDto.builder()
                        .playersOnline(417)
                        .build())
                .tournament(HomeSummaryResponse.TournamentDto.builder()
                        .title("Great Tournament")
                        .endsAt(LocalDateTime.now().plusHours(25).format(ISO_FORMATTER))
                        .build())
                .build();
    }

    private Account extractAccount(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Account account)) {
            throw new RuntimeException("Unauthorized");
        }
        return account;
    }
}

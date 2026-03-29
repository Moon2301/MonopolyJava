package com.game.monopoly.service;

import com.game.monopoly.dto.UserMeSummaryResponse;
import com.game.monopoly.model.enums.GameStatus;
import com.game.monopoly.model.inGameData.Game;
import com.game.monopoly.model.inGameData.GamePlayer;
import com.game.monopoly.model.metaData.Account;
import com.game.monopoly.model.metaData.UserProfile;
import com.game.monopoly.repository.GamePlayerRepository;
import com.game.monopoly.repository.GameRepository;
import com.game.monopoly.repository.HeroRepository;
import com.game.monopoly.repository.AccountRepository;
import com.game.monopoly.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserMeService {

    private static final String DEFAULT_AVATAR = "/images/avatar-default.png";

    private final AccountRepository accountRepository;
    private final UserProfileRepository userProfileRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final GameRepository gameRepository;
    private final HeroRepository heroRepository;

    public UserMeSummaryResponse getSummary(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        UserProfile profile = userProfileRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new RuntimeException("UserProfile not found"));

        Long userProfileId = profile.getUserProfileId();

        int matches = 0;
        int wins = 0;
        long totalWonAssets = 0L;

        GamePlayer latestFinishedGamePlayerForHero = null;
        for (GamePlayer gamePlayer : gamePlayerRepository.findByUserProfileId(userProfileId)) {
            Game game = gameRepository.findById(gamePlayer.getGameId()).orElse(null);
            if (game == null || game.getStatus() != GameStatus.FINISHED) {
                continue;
            }

            // win/loss summary
            matches++;
            Long winnerPlayerId = game.getWinnerPlayerId();
            if (winnerPlayerId != null && Objects.equals(winnerPlayerId, gamePlayer.getGamePlayerId())) {
                wins++;
                if (gamePlayer.getBalance() != null) {
                    totalWonAssets += gamePlayer.getBalance();
                }
            }

            // hero used: pick latest finished game by endedAt (fallback startedAt)
            LocalDateTime end = game.getEndedAt();
            if (end == null) {
                end = game.getStartedAt();
            }
            if (end != null) {
                if (latestFinishedGamePlayerForHero == null) {
                    latestFinishedGamePlayerForHero = gamePlayer;
                } else {
                    Game latestGame = gameRepository.findById(latestFinishedGamePlayerForHero.getGameId()).orElse(null);
                    if (latestGame != null) {
                        LocalDateTime latestEnd = latestGame.getEndedAt() != null ? latestGame.getEndedAt() : latestGame.getStartedAt();
                        if (latestEnd != null && end.isAfter(latestEnd)) {
                            latestFinishedGamePlayerForHero = gamePlayer;
                        }
                    }
                }
            }
        }

        int winRate = matches == 0 ? 0 : (int) Math.round(wins * 100.0 / matches);

        String avatarUrl = profile.getAvatarUrl();
        if (avatarUrl == null || avatarUrl.isBlank()) {
            avatarUrl = DEFAULT_AVATAR;
        }

        Integer equippedCharacterId = null;
        String equippedCharacterName = null;
        String equippedCharacterImageUrl = null;

        if (latestFinishedGamePlayerForHero != null && latestFinishedGamePlayerForHero.getCharacterId() != null) {
            equippedCharacterId = latestFinishedGamePlayerForHero.getCharacterId();
            var hero = heroRepository.findById(equippedCharacterId).orElse(null);
            if (hero != null) {
                equippedCharacterName = hero.getName();
                equippedCharacterImageUrl = "/images/heroes/" + hero.getName().toLowerCase().replace(" ", "-") + ".png";
            }
        }

        return UserMeSummaryResponse.builder()
                .username(profile.getUsername())
                .avatarUrl(avatarUrl)
                .coins(profile.getGold())
                .tickets(profile.getDiamonds())
                .rankTier(profile.getRankTier())
                .matches(matches)
                .winRate(winRate)
                .totalWonAssets(totalWonAssets)
                .equippedCharacterId(equippedCharacterId)
                .equippedCharacterName(equippedCharacterName)
                .equippedCharacterImageUrl(equippedCharacterImageUrl)
                .build();
    }

    @Transactional
    public void updateUsername(Long accountId, String newUsername) {
        if (newUsername == null || newUsername.trim().isBlank()) {
            throw new RuntimeException("Tên người dùng không được bỏ trống");
        }
        String trimmedName = newUsername.trim();
        if (trimmedName.length() > 20) {
            throw new RuntimeException("Tên người dùng quá dài (tối đa 20 ký tự)");
        }
        
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));
        UserProfile profile = userProfileRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new RuntimeException("Cấu hình người dùng không tồn tại"));

        if (!trimmedName.equalsIgnoreCase(profile.getUsername()) && userProfileRepository.existsByUsername(trimmedName)) {
            throw new RuntimeException("Tên người dùng đã được sử dụng");
        }

        profile.setUsername(trimmedName);
        userProfileRepository.save(profile);
    }

    public String updateAvatar(Long accountId, MultipartFile avatar) {
        if (avatar == null || avatar.isEmpty()) {
            throw new RuntimeException("Avatar is required");
        }

        if (avatar.getSize() > 5 * 1024 * 1024) { // 5MB
            throw new RuntimeException("Avatar is too large (max 5MB)");
        }

        String contentType = avatar.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("File is not an image");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        UserProfile profile = userProfileRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new RuntimeException("UserProfile not found"));

        String ext = guessExtension(contentType, avatar.getOriginalFilename());

        Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads", "avatars");
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create upload directory", e);
        }

        String filename = account.getAccountId() + "-" + System.currentTimeMillis() + "." + ext;
        Path target = uploadDir.resolve(filename);
        try {
            avatar.transferTo(target);
        } catch (IOException e) {
            throw new RuntimeException("Cannot save avatar", e);
        }

        String avatarUrl = "/uploads/avatars/" + filename;
        profile.setAvatarUrl(avatarUrl);
        userProfileRepository.save(profile);

        return avatarUrl;
    }

    private String guessExtension(String contentType, String originalFilename) {
        if (contentType == null) {
            return "png";
        }
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/jpg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            default -> {
                if (originalFilename != null && originalFilename.contains(".")) {
                    String suffix = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
                    if (!suffix.isBlank()) {
                        yield suffix;
                    }
                }
                yield "png";
            }
        };
    }

}


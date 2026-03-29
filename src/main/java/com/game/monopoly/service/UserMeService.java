package com.game.monopoly.service;

import com.game.monopoly.dto.UserMeSummaryResponse;
import com.game.monopoly.model.enums.GameStatus;
import com.game.monopoly.model.inGameData.Game;
import com.game.monopoly.model.inGameData.GamePlayer;
import com.game.monopoly.model.metaData.Account;
import com.game.monopoly.model.metaData.Hero;
import com.game.monopoly.model.metaData.UserProfile;
import com.game.monopoly.repository.GamePlayerRepository;
import com.game.monopoly.repository.GameRepository;
import com.game.monopoly.repository.HeroRepository;
import com.game.monopoly.repository.AccountRepository;
import com.game.monopoly.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserMeService {

    private static final String DEFAULT_AVATAR = "/images/avatar-default.png";
    /** Phí đổi tên hiển thị (xu). */
    private static final long DISPLAY_NAME_CHANGE_COST = 1000L;

    private final AccountRepository accountRepository;
    private final UserProfileRepository userProfileRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final GameRepository gameRepository;
    private final HeroRepository heroRepository;
    private final HeroOwnershipService heroOwnershipService;

    public UserMeSummaryResponse getSummary(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        UserProfile profile = userProfileRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new RuntimeException("UserProfile not found"));

        Long userProfileId = profile.getUserProfileId();

        int matches = 0;
        int wins = 0;
        long totalWonAssets = 0L;

        for (GamePlayer gamePlayer : gamePlayerRepository.findByUserProfileId(userProfileId)) {
            Game game = gameRepository.findById(gamePlayer.getGameId()).orElse(null);
            if (game == null || game.getStatus() != GameStatus.FINISHED) {
                continue;
            }

            matches++;
            Long winnerPlayerId = game.getWinnerPlayerId();
            if (winnerPlayerId != null && Objects.equals(winnerPlayerId, gamePlayer.getGamePlayerId())) {
                wins++;
                if (gamePlayer.getBalance() != null) {
                    totalWonAssets += gamePlayer.getBalance();
                }
            }
        }

        int winRate = matches == 0 ? 0 : (int) Math.round(wins * 100.0 / matches);

        String avatarUrl = profile.getAvatarUrl();
        if (avatarUrl == null || avatarUrl.isBlank()) {
            avatarUrl = DEFAULT_AVATAR;
        }

        Hero displayHero = resolveDisplayHero(profile);
        Integer equippedCharacterId = null;
        String equippedCharacterName = null;
        String equippedCharacterImageUrl = null;
        if (displayHero != null) {
            equippedCharacterId = displayHero.getCharacterId();
            equippedCharacterName = displayHero.getName();
            equippedCharacterImageUrl =
                    "/images/heroes/"
                            + displayHero.getName().toLowerCase().replace(" ", "-")
                            + ".png";
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

    /**
     * Nhân vật hiển thị hồ sơ: {@link UserProfile#getDefaultCharacterId()}, nếu null hoặc không còn tồn tại
     * thì hero mở mặc định đầu tiên, sau đó hero tồn tại sớm nhất theo {@code character_id}.
     */
    private Hero resolveDisplayHero(UserProfile profile) {
        Integer id = profile.getDefaultCharacterId();
        if (id != null) {
            Optional<Hero> byId = heroRepository.findById(id);
            if (byId.isPresent()) {
                return byId.get();
            }
        }
        return heroRepository
                .findFirstByDefaultUnlockedTrueOrderByCharacterIdAsc()
                .or(
                        () ->
                                heroRepository
                                        .findAll(Sort.by(Sort.Direction.ASC, "characterId"))
                                        .stream()
                                        .findFirst())
                .orElse(null);
    }

    @Transactional
    public UserMeSummaryResponse changeDisplayName(Long accountId, String rawNewUsername) {
        if (accountId == null) {
            throw new RuntimeException("Cần đăng nhập");
        }
        if (rawNewUsername == null || rawNewUsername.isBlank()) {
            throw new RuntimeException("Tên hiển thị không được để trống");
        }
        String trimmed = rawNewUsername.trim();
        if (trimmed.length() < 2 || trimmed.length() > 80) {
            throw new RuntimeException("Tên hiển thị từ 2 đến 80 ký tự");
        }

        Account account =
                accountRepository
                        .findById(accountId)
                        .orElseThrow(() -> new RuntimeException("Account not found"));
        UserProfile profile =
                userProfileRepository
                        .findByAccount_AccountId(account.getAccountId())
                        .orElseThrow(() -> new RuntimeException("UserProfile not found"));

        if (trimmed.equals(profile.getUsername())) {
            throw new RuntimeException("Tên mới phải khác tên hiện tại");
        }

        Optional<UserProfile> taken = userProfileRepository.findByUsername(trimmed);
        if (taken.isPresent()
                && !taken.get().getUserProfileId().equals(profile.getUserProfileId())) {
            throw new RuntimeException("Tên này đã được người khác sử dụng");
        }

        long gold = profile.getGold() == null ? 0L : profile.getGold();
        if (gold < DISPLAY_NAME_CHANGE_COST) {
            throw new RuntimeException(
                    "Đổi tên cần " + DISPLAY_NAME_CHANGE_COST + " xu (bạn đang có " + gold + " xu)");
        }

        profile.setGold(gold - DISPLAY_NAME_CHANGE_COST);
        profile.setUsername(trimmed);
        userProfileRepository.save(profile);

        return getSummary(accountId);
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

    @Transactional
    public void equipHero(Long accountId, Integer heroId) {
        if (heroId == null) {
            throw new RuntimeException("Hero ID is required");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        UserProfile profile = userProfileRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new RuntimeException("UserProfile not found"));

        // Kiểm tra xem user có sở hữu hero này không
        // Giả sử có heroOwnershipService hoặc kiểm tra trực tiếp qua repository
        // Do UserMeService không inject HeroOwnershipService, tôi sẽ dùng HeroRepository và logic mặc định hoặc kiểm tra đơn giản
        
        Hero hero = heroRepository.findById(heroId)
                .orElseThrow(() -> new RuntimeException("Hero không tồn tại"));

        if (!heroOwnershipService.isHeroOwned(accountId, hero.getCharacterId())) {
            throw new RuntimeException("Bạn không sở hữu nhân vật này");
        }
        
        profile.setDefaultCharacterId(hero.getCharacterId());
        userProfileRepository.save(profile);
    }

    @Transactional
    public UserMeSummaryResponse setCurrentHero(Long accountId, Integer heroId) {
        // dùng lại logic đã có
        equipHero(accountId, heroId);

        // trả về summary cho controller
        return getSummary(accountId);
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


package com.game.monopoly.service.skill;

import com.game.monopoly.dto.SkillActivateRequest;
import com.game.monopoly.model.enums.GameStatus;
import com.game.monopoly.model.inGameData.Game;
import com.game.monopoly.model.inGameData.GamePlayer;
import com.game.monopoly.model.metaData.Account;
import com.game.monopoly.model.metaData.Skill;
import com.game.monopoly.model.metaData.UserProfile;
import com.game.monopoly.repository.AccountRepository;
import com.game.monopoly.repository.CharacterSkillRepository;
import com.game.monopoly.repository.GamePlayerRepository;
import com.game.monopoly.repository.GameRepository;
import com.game.monopoly.repository.SkillRepository;
import com.game.monopoly.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Kích hoạt kỹ năng chủ động trong ván — hiệu ứng chi tiết theo {@link Skill#getEffectType()} (bổ sung dần).
 */
@Service
@RequiredArgsConstructor
public class SkillActivationService {

    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final CharacterSkillRepository characterSkillRepository;
    private final SkillRepository skillRepository;
    private final UserProfileRepository userProfileRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public String performActivate(
            Long gameId,
            Long accountId,
            SkillActivateRequest request,
            Consumer<String> gameLogSink) {
        if (request == null || request.getSkillId() == null) {
            throw new RuntimeException("Cần skillId");
        }
        Game game =
                gameRepository
                        .findById(gameId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy game"));
        if (game.getStatus() != GameStatus.PLAYING) {
            throw new RuntimeException("Game không đang chơi");
        }
        GamePlayer player = getCurrentTurnPlayer(game);
        validateHumanTurn(player, accountId);
        if ("INSOLVENT".equalsIgnoreCase(game.getTurnState())) {
            throw new RuntimeException("Đang nợ — không dùng kỹ năng");
        }
        String ts = game.getTurnState() == null ? "" : game.getTurnState();
        if (!"WAIT_ROLL".equalsIgnoreCase(ts) && !"ACTION_REQUIRED".equalsIgnoreCase(ts)) {
            throw new RuntimeException("Không thể dùng kỹ năng lúc này");
        }

        if (player.getCharacterId() == null) {
            throw new RuntimeException("Bạn chưa chọn nhân vật");
        }
        Skill skill =
                skillRepository
                        .findById(request.getSkillId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy kỹ năng"));

        boolean owns =
                characterSkillRepository.findByHero_CharacterId(player.getCharacterId()).stream()
                        .anyMatch(
                                cs ->
                                        cs.getSkill() != null
                                                && Objects.equals(
                                                        cs.getSkill().getSkillId(), skill.getSkillId()));
        if (!owns) {
            throw new RuntimeException("Nhân vật của bạn không có kỹ năng này");
        }

        boolean passive =
                skill.getTriggerType() != null
                        && skill.getTriggerType().toUpperCase(Locale.ROOT).contains("PASSIVE");
        if (passive) {
            throw new RuntimeException("Kỹ năng thụ động không cần kích hoạt");
        }

        int cdRem =
                player.getSkillCooldownRemaining() == null ? 0 : player.getSkillCooldownRemaining();
        if (cdRem > 0) {
            throw new RuntimeException("Kỹ năng đang hồi (" + cdRem + " lượt)");
        }

        applyEffect(skill, player, game);

        int cd = skill.getCooldown() == null ? 0 : skill.getCooldown();
        player.setSkillCooldownRemaining(Math.max(0, cd));
        gamePlayerRepository.save(player);

        if (gameLogSink != null) {
            gameLogSink.accept(
                    displayNameForPlayer(player) + " kích hoạt \"" + skill.getName() + "\".");
        }

        return "Đã kích hoạt " + skill.getName();
    }

    /** Hook hiệu ứng — mở rộng theo {@link Skill#getEffectType()} và luật GamePlayService. */
    private void applyEffect(Skill skill, GamePlayer player, Game game) {
        String effect =
                skill.getEffectType() == null ? "" : skill.getEffectType().toUpperCase(Locale.ROOT);
        switch (effect) {
            case "SET_MOVE_RANGE":
            case "EXTRA_RANDOM_MOVE":
            case "DUEL_DICE":
            case "RESET_PROPERTY_OWNER":
            case "MARK_AND_BUYBACK":
                /* TODO: nối engine di chuyển / đấu / tài sản */
                break;
            default:
                break;
        }
    }

    private GamePlayer getCurrentTurnPlayer(Game game) {
        Integer order = game.getCurrentPlayerOrder();
        return gamePlayerRepository.findByGameIdOrderByTurnOrderAsc(game.getGameId()).stream()
                .filter(p -> Objects.equals(p.getTurnOrder(), order))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người chơi hiện tại"));
    }

    private void validateHumanTurn(GamePlayer currentTurnPlayer, Long accountId) {
        if (Boolean.TRUE.equals(currentTurnPlayer.getIsBot())) {
            throw new RuntimeException("Không phải lượt người");
        }
        if (accountId == null) {
            throw new RuntimeException("Cần đăng nhập");
        }
        Account account =
                accountRepository
                        .findById(accountId)
                        .orElseThrow(() -> new RuntimeException("Account not found"));
        UserProfile profile =
                userProfileRepository
                        .findByAccount_AccountId(account.getAccountId())
                        .orElseThrow(() -> new RuntimeException("UserProfile not found"));
        if (!Objects.equals(currentTurnPlayer.getUserProfileId(), profile.getUserProfileId())) {
            throw new RuntimeException("Không phải lượt của bạn");
        }
    }

    private String displayNameForPlayer(GamePlayer player) {
        if (Boolean.TRUE.equals(player.getIsBot())) {
            return "Bot " + (player.getTurnOrder() != null ? player.getTurnOrder() : "");
        }
        if (player.getUserProfileId() != null) {
            Optional<UserProfile> profile = userProfileRepository.findById(player.getUserProfileId());
            if (profile.isPresent()
                    && profile.get().getUsername() != null
                    && !profile.get().getUsername().isBlank()) {
                return profile.get().getUsername();
            }
        }
        return "Người chơi " + (player.getTurnOrder() != null ? player.getTurnOrder() : "");
    }
}

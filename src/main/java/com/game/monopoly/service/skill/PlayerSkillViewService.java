package com.game.monopoly.service.skill;

import com.game.monopoly.dto.GameStateResponse;
import com.game.monopoly.model.inGameData.GamePlayer;
import com.game.monopoly.model.metaData.CharacterSkill;
import com.game.monopoly.model.metaData.Skill;
import com.game.monopoly.repository.CharacterSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PlayerSkillViewService {

    private final CharacterSkillRepository characterSkillRepository;

    public List<GameStateResponse.PlayerSkillDto> buildSkillDtos(GamePlayer player) {
        if (player.getCharacterId() == null) {
            return List.of();
        }
        int cdRem =
                player.getSkillCooldownRemaining() == null ? 0 : player.getSkillCooldownRemaining();
        List<CharacterSkill> links = characterSkillRepository.findByHero_CharacterId(player.getCharacterId());
        List<GameStateResponse.PlayerSkillDto> out = new ArrayList<>();
        for (CharacterSkill cs : links) {
            Skill s = cs.getSkill();
            if (s == null) {
                continue;
            }
            boolean passive =
                    s.getTriggerType() != null
                            && s.getTriggerType().toUpperCase(Locale.ROOT).contains("PASSIVE");
            boolean ready = passive || cdRem <= 0;
            out.add(
                    GameStateResponse.PlayerSkillDto.builder()
                            .skillId(s.getSkillId())
                            .name(s.getName())
                            .description(s.getEffectFormula())
                            .triggerType(s.getTriggerType())
                            .cooldownTurns(s.getCooldown())
                            .cooldownRemaining(passive ? 0 : cdRem)
                            .passiveActive(passive)
                            .readyToActivate(!passive && ready)
                            .build());
        }
        return out;
    }
}

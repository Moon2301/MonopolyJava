package com.game.monopoly.service;

import com.game.monopoly.model.metaData.Hero;
import com.game.monopoly.repository.HeroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HeroOwnershipService {

    private final HeroRepository heroRepository;

    public Set<Integer> getOwnedHeroIds(Long accountId) {
        // For now, players own all heroes that are unlocked by default.
        // A complete ownership system with database mapping can be integrated later.
        return heroRepository.findAll().stream()
                .filter(Hero::getDefaultUnlocked)
                .map(Hero::getCharacterId)
                .collect(Collectors.toSet());
    }

    public boolean isHeroOwned(Long accountId, Integer heroId) {
        return getOwnedHeroIds(accountId).contains(heroId);
    }
}

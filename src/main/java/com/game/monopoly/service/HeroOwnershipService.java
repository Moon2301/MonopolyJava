package com.game.monopoly.service;

import com.game.monopoly.model.metaData.Hero;
import com.game.monopoly.model.metaData.UserOwnedHero;
import com.game.monopoly.repository.HeroRepository;
import com.game.monopoly.repository.UserOwnedHeroRepository;
import com.game.monopoly.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HeroOwnershipService {

    private final HeroRepository heroRepository;
    private final UserOwnedHeroRepository userOwnedHeroRepository;
    private final UserProfileRepository userProfileRepository;

    public Set<Integer> getOwnedHeroIds(Long accountId) {
        // 1. Phải có ít nhất các hero mặc định (unlocked by default)
        Set<Integer> owned = heroRepository.findAll().stream()
                .filter(hero -> Boolean.TRUE.equals(hero.getDefaultUnlocked()))
                .map(Hero::getCharacterId)
                .collect(Collectors.toSet());

        // 2. Lấy thêm các hero người dùng đã mua từ database
        userProfileRepository.findByAccount_AccountId(accountId).ifPresent(profile -> {
            List<UserOwnedHero> bought = userOwnedHeroRepository.findByUserProfile_UserProfileId(profile.getUserProfileId());
            for (UserOwnedHero bh : bought) {
                if (bh.getHero() != null) {
                    owned.add(bh.getHero().getCharacterId());
                }
            }
        });

        return owned;
    }

    public boolean isHeroOwned(Long accountId, Integer heroId) {
        if (heroId == null) return false;
        
        // Kiểm tra xem hero có được mở mặc định không
        boolean isDefault = heroRepository.findById(heroId)
                .map(h -> Boolean.TRUE.equals(h.getDefaultUnlocked()))
                .orElse(false);
        if (isDefault) return true;

        // Kiểm tra trong bảng UserOwnedHero
        return userProfileRepository.findByAccount_AccountId(accountId)
                .map(profile -> userOwnedHeroRepository.existsByUserProfile_UserProfileIdAndHero_CharacterId(
                        profile.getUserProfileId(), heroId))
                .orElse(false);
    }
}

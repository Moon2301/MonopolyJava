package com.game.monopoly.service;

import com.game.monopoly.dto.HeroPurchaseResponse;
import com.game.monopoly.dto.ShopStateResponse;
import com.game.monopoly.model.enums.CurrencyType;
import com.game.monopoly.model.metaData.CurrencyLedger;
import com.game.monopoly.model.metaData.Hero;
import com.game.monopoly.model.metaData.UserOwnedHero;
import com.game.monopoly.model.metaData.UserProfile;
import com.game.monopoly.repository.CurrencyLedgerRepository;
import com.game.monopoly.repository.HeroRepository;
import com.game.monopoly.repository.UserOwnedHeroRepository;
import com.game.monopoly.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HeroShopService {

    private final UserProfileRepository userProfileRepository;
    private final HeroRepository heroRepository;
    private final UserOwnedHeroRepository userOwnedHeroRepository;
    private final CurrencyLedgerRepository currencyLedgerRepository;
    private final HeroOwnershipService heroOwnershipService;

    public ShopStateResponse getShopState(Long accountId) {
        UserProfile profile = userProfileRepository.findByAccount_AccountId(accountId)
                .orElseThrow(() -> new RuntimeException("UserProfile not found"));
        List<Integer> ownedHeroIds = heroOwnershipService.getOwnedHeroIds(accountId).stream().toList();

        return ShopStateResponse.builder()
                .coins(profile.getGold())
                .ownedHeroIds(ownedHeroIds)
                .build();
    }

    @Transactional
    public HeroPurchaseResponse purchaseHero(Long accountId, Integer heroId) {
        if (heroId == null) {
            throw new RuntimeException("heroId is required");
        }

        UserProfile profile = userProfileRepository.findByAccount_AccountId(accountId)
                .orElseThrow(() -> new RuntimeException("UserProfile not found"));

        Hero hero = heroRepository.findById(heroId)
                .orElseThrow(() -> new RuntimeException("Hero not found"));

        boolean alreadyOwned = heroOwnershipService.isHeroOwned(accountId, heroId);
        if (alreadyOwned) {
            return HeroPurchaseResponse.builder()
                    .heroId(heroId)
                    .remainingCoins(profile.getGold())
                    .purchased(false)
                    .message("Bạn đã sở hữu nhân vật này")
                    .build();
        }

        long price = hero.getPrice() == null ? 0L : hero.getPrice();
        if (profile.getGold() < price) {
            throw new RuntimeException("Không đủ xu để mua nhân vật");
        }

        // 1. Tính toán và cập nhật số dư mới
        long newBalance = profile.getGold() - price;
        profile.setGold(newBalance);
        userProfileRepository.save(profile);

        // 2. Thêm nhân vật vào kho đồ của User
        UserOwnedHero userOwnedHero = new UserOwnedHero();
        userOwnedHero.setUserProfile(profile);
        userOwnedHero.setHero(hero);
        userOwnedHeroRepository.save(userOwnedHero);

        // 3. Ghi lại lịch sử giao dịch (Đã có setBalanceAfter)
        CurrencyLedger ledger = new CurrencyLedger();
        ledger.setUserProfile(profile);
        ledger.setCurrencyType(CurrencyType.GOLD);
        ledger.setAmount(-price);
        ledger.setBalanceAfter(newBalance); // <-- Dữ liệu được gán tại đây
        ledger.setReasonType("HERO_PURCHASE");
        ledger.setReferenceId(heroId.longValue());

        // Lưu xuống DB
        currencyLedgerRepository.save(ledger);

        return HeroPurchaseResponse.builder()
                .heroId(heroId)
                .remainingCoins(newBalance)
                .purchased(true)
                .message("Mua nhân vật thành công")
                .build();
    }
}
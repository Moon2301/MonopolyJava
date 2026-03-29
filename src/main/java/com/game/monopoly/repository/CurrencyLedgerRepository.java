package com.game.monopoly.repository;

import com.game.monopoly.model.metaData.CurrencyLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CurrencyLedgerRepository extends JpaRepository<CurrencyLedger, Long> {
    List<CurrencyLedger> findByUserProfile_UserProfileId(Long userProfileId);
    boolean existsByReasonTypeAndReferenceId(String reasonType, Long referenceId);
}

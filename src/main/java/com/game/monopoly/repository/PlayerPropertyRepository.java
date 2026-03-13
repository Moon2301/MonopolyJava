package com.game.monopoly.repository;

import com.game.monopoly.model.inGameData.PlayerProperty;
import com.game.monopoly.model.inGameData.PlayerPropertyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerPropertyRepository extends JpaRepository<PlayerProperty, PlayerPropertyId> {
    List<PlayerProperty> findByGame_GameId(Long gameId);
}

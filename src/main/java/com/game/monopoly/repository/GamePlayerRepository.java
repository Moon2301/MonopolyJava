package com.game.monopoly.repository;

import com.game.monopoly.model.inGameData.GamePlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GamePlayerRepository extends JpaRepository<GamePlayer, Long> {

    List<GamePlayer> findByGameId(Long gameId);

}
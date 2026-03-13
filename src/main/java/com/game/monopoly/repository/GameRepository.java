package com.game.monopoly.repository;

import com.game.monopoly.model.inGameData.Game;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Long> {
}
package com.game.monopoly.service;

import com.game.monopoly.model.inGameData.Game;
import com.game.monopoly.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;

    public List<Game> getAllGames() {
        return gameRepository.findAll();
    }

    public Game getGameById(Long id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Game not found"));
    }

    public Game createGame(Game game) {
        return gameRepository.save(game);
    }

    public Game updateGame(Long id, Game newGame) {

        Game game = getGameById(id);

        game.setStatus(newGame.getStatus());
        game.setMaxPlayers(newGame.getMaxPlayers());
        game.setTurnState(newGame.getTurnState());
        game.setCurrentTurn(newGame.getCurrentTurn());
        game.setCurrentPlayerOrder(newGame.getCurrentPlayerOrder());

        return gameRepository.save(game);
    }

    public void deleteGame(Long id) {
        gameRepository.deleteById(id);
    }
}
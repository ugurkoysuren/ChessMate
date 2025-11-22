package com.railway.blindchess.controller;

import com.railway.blindchess.chess.ChessGameManager;
import com.railway.blindchess.model.ChessGameState;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChessController {
    private final ChessGameManager gameManager;

    public ChessController(ChessGameManager gameManager) { this.gameManager = gameManager; }

    @GetMapping("/games")
    public Map<String, ChessGameState> getAllGames() { return gameManager.getAllGames(); }

    @GetMapping("/game/{gameId}")
    public ChessGameState getGame(@PathVariable String gameId) { return gameManager.getGame(gameId); }
}

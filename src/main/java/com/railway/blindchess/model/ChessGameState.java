package com.railway.blindchess.model;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;

import java.util.ArrayList;
import java.util.List;

public class ChessGameState {
    private final String gameId;
    private final String playerId;
    private final String playerName;
    private final Board board;
    private final List<String> moveHistory;
    private final List<MoveAnnotation> moveAnnotations;
    private final Side playerSide;
    private boolean gameOver;
    private String result;
    private long lastMoveTime;
    private Integer currentEvaluation;

    public ChessGameState(String gameId, String playerId, String playerName, Side playerSide) {
        this.gameId = gameId;
        this.playerId = playerId;
        this.playerName = playerName;
        this.board = new Board();
        this.moveHistory = new ArrayList<>();
        this.moveAnnotations = new ArrayList<>();
        this.playerSide = playerSide;
        this.gameOver = false;
        this.lastMoveTime = System.currentTimeMillis();
        this.currentEvaluation = 0;
    }

    public String getGameId() {
        return gameId;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public Board getBoard() {
        return board;
    }

    public List<String> getMoveHistory() {
        return moveHistory;
    }

    public List<MoveAnnotation> getMoveAnnotations() {
        return moveAnnotations;
    }

    public Side getPlayerSide() {
        return playerSide;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public long getLastMoveTime() {
        return lastMoveTime;
    }

    public void setLastMoveTime(long lastMoveTime) {
        this.lastMoveTime = lastMoveTime;
    }

    public Integer getCurrentEvaluation() {
        return currentEvaluation;
    }

    public void setCurrentEvaluation(Integer currentEvaluation) {
        this.currentEvaluation = currentEvaluation;
    }

    public String getFen() {
        return board.getFen();
    }

    public String getPgn() {
        return String.join(" ", moveHistory);
    }

    public MoveAnnotation getLastMoveAnnotation() {
        if (moveAnnotations.isEmpty()) return null;
        return moveAnnotations.get(moveAnnotations.size() - 1);
    }
}

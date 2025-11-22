package com.railway.blindchess.chess;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveGenerator;
import com.railway.blindchess.model.ChessGameState;
import com.railway.blindchess.model.MoveAnnotation;
import com.railway.blindchess.model.MoveQuality;
import com.railway.blindchess.websocket.ChessWebSocketHandler;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChessGameManager {
    private final Map<String, ChessGameState> activeGames = new ConcurrentHashMap<>();
    private final StockfishEngine stockfish;
    private final ChessWebSocketHandler webSocketHandler;

    public ChessGameManager(StockfishEngine stockfish, ChessWebSocketHandler webSocketHandler) {
        this.stockfish = stockfish;
        this.webSocketHandler = webSocketHandler;
    }

    @PostConstruct
    public void init() {
        try {
            stockfish.start();
            System.out.println("Stockfish engine started successfully");
        } catch (IOException e) {
            System.err.println("Failed to start Stockfish: " + e.getMessage());
            System.err.println("The application will continue, but Stockfish features will be unavailable.");
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            stockfish.stop();
        } catch (IOException e) {
            System.err.println("Error stopping Stockfish: " + e.getMessage());
        }
    }

    public ChessGameState createGame(String playerId, String playerName, Side playerSide) {
        ChessGameState game = new ChessGameState(playerId, playerId, playerName, playerSide);
        activeGames.put(playerId, game);
        broadcastGameState(game);
        if (playerSide == Side.BLACK) {
            try {
                if (stockfish.isRunning()) {
                    makeStockfishMove(playerId);
                } else {
                    System.err.println("Stockfish is not running, skipping opening move.");
                }
            } catch (Exception e) {
                System.err.println("Error making Stockfish opening move: " + e.getMessage());
            }
        }
        return game;
    }

    public String makePlayerMove(String gameId, String moveNotation) throws Exception {
        ChessGameState game = activeGames.get(gameId);
        if (game == null) throw new Exception("Game not found");
        if (game.isGameOver()) throw new Exception("Game is already over");

        Board board = game.getBoard();
        if (board.getSideToMove() != game.getPlayerSide()) throw new Exception("Not your turn!");

        Integer evalBefore = game.getCurrentEvaluation();
        StockfishEngine.EvaluationResult bestMoveResult = null;
        if (stockfish.isRunning()) {
            try {
                bestMoveResult = stockfish.getBestMoveWithEvaluation(game.getFen(), 1000);
            } catch (Exception e) {
                System.err.println("Error getting best move evaluation: " + e.getMessage());
            }
        }

        Move move = parseMove(board, moveNotation);
        if (move == null) throw new Exception("Invalid move: " + moveNotation);

        board.doMove(move);
        game.getMoveHistory().add(moveNotation);
        game.setLastMoveTime(System.currentTimeMillis());

        Integer evalAfter = null;
        if (stockfish.isRunning()) {
            try {
                StockfishEngine.EvaluationResult afterEval = stockfish.getBestMoveWithEvaluation(game.getFen(), 500);
                if (afterEval != null && afterEval.getCentipawns() != null) {
                    evalAfter = afterEval.getCentipawns();
                    game.setCurrentEvaluation(evalAfter);
                }
            } catch (Exception e) {
                System.err.println("Error getting position evaluation: " + e.getMessage());
            }
        }

        MoveQuality quality = determineMoveQuality(moveNotation, bestMoveResult != null ? bestMoveResult.getBestMove() : null, evalBefore, evalAfter, game.getPlayerSide());
        game.getMoveAnnotations().add(new MoveAnnotation(moveNotation, evalBefore, evalAfter, quality, false));

        checkGameState(game);
        broadcastGameState(game);

        if (!game.isGameOver() && stockfish.isRunning()) {
            try {
                return makeStockfishMove(gameId);
            } catch (Exception e) {
                return "Error making Stockfish move: " + e.getMessage();
            }
        }
        
        if (game.isGameOver()) {
            return "Game over: " + game.getResult();
        }
        return "Move made successfully";
    }

    private String makeStockfishMove(String gameId) throws Exception {
        if (!stockfish.isRunning()) {
            throw new Exception("Stockfish is not running");
        }
        ChessGameState game = activeGames.get(gameId);
        if (game == null || game.isGameOver()) return null;

        Integer evalBefore = game.getCurrentEvaluation();
        StockfishEngine.EvaluationResult result = stockfish.getBestMoveWithEvaluation(game.getFen(), 1000);
        String bestMove = result != null ? result.getBestMove() : null;

        if (bestMove != null) {
            Board board = game.getBoard();
            Move move = parseMove(board, bestMove);
            if (move != null) {
                board.doMove(move);
                game.getMoveHistory().add(bestMove);
                game.setLastMoveTime(System.currentTimeMillis());

                Integer evalAfter = null;
                try {
                    StockfishEngine.EvaluationResult afterEval = stockfish.getBestMoveWithEvaluation(game.getFen(), 500);
                    if (afterEval != null && afterEval.getCentipawns() != null) {
                        evalAfter = afterEval.getCentipawns();
                        game.setCurrentEvaluation(evalAfter);
                    }
                } catch (Exception e) {
                    System.err.println("Error getting evaluation after Stockfish move: " + e.getMessage());
                }

                game.getMoveAnnotations().add(new MoveAnnotation(bestMove, evalBefore, evalAfter, MoveQuality.BEST, false));
                checkGameState(game);
                broadcastGameState(game);
                return bestMove;
            }
        }
        throw new Exception("Stockfish failed to make a move");
    }

    private MoveQuality determineMoveQuality(String playerMove, String bestMove, Integer evalBefore, Integer evalAfter, Side playerSide) {
        if (evalBefore == null || evalAfter == null) return MoveQuality.GOOD;
        if (playerMove.equalsIgnoreCase(bestMove)) return MoveQuality.BEST;
        int evalChange = playerSide == Side.WHITE ? evalAfter - evalBefore : evalBefore - evalAfter;
        if (evalChange >= -25) return MoveQuality.BEST;
        if (evalChange >= -50) return MoveQuality.GOOD;
        return MoveQuality.fromEvalLoss(Math.abs(Math.min(evalChange, 0)));
    }

    private Move parseMove(Board board, String moveStr) {
        try {
            for (Move move : MoveGenerator.generateLegalMoves(board)) {
                if (move.toString().equalsIgnoreCase(moveStr)) return move;
            }
        } catch (Exception e) {
            System.err.println("Error parsing move: " + e.getMessage());
        }
        return null;
    }

    private void checkGameState(ChessGameState game) {
        Board board = game.getBoard();
        if (board.isMated()) {
            game.setGameOver(true);
            game.setResult((board.getSideToMove() == Side.WHITE ? Side.BLACK : Side.WHITE) + " wins by checkmate!");
        } else if (board.isDraw()) {
            game.setGameOver(true);
            game.setResult("Draw");
        } else if (board.isStaleMate()) {
            game.setGameOver(true);
            game.setResult("Stalemate");
        } else if (board.isInsufficientMaterial()) {
            game.setGameOver(true);
            game.setResult("Draw by insufficient material");
        }
    }

    public ChessGameState getGame(String gameId) {
        return activeGames.get(gameId);
    }

    public List<String> getLegalMoves(String gameId) {
        ChessGameState game = activeGames.get(gameId);
        return game == null ? List.of() : MoveGenerator.generateLegalMoves(game.getBoard()).stream().map(Move::toString).toList();
    }

    public void resignGame(String gameId) {
        ChessGameState game = activeGames.get(gameId);
        if (game != null) {
            game.setGameOver(true);
            game.setResult(game.getPlayerName() + " resigned. Stockfish wins!");
            broadcastGameState(game);
        }
    }

    private void broadcastGameState(ChessGameState game) {
        webSocketHandler.broadcastGameState(game);
    }

    public Map<String, ChessGameState> getAllGames() {
        return activeGames;
    }
}

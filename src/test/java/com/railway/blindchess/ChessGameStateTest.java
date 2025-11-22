package com.railway.blindchess;

import com.github.bhlangonijr.chesslib.Side;
import com.railway.blindchess.model.ChessGameState;
import com.railway.blindchess.model.MoveAnnotation;
import com.railway.blindchess.model.MoveQuality;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChessGameStateTest {

    @Test
    void testGameCreation() {
        ChessGameState game = new ChessGameState("game1", "player1", "TestPlayer", Side.WHITE);

        assertEquals("game1", game.getGameId());
        assertEquals("player1", game.getPlayerId());
        assertEquals("TestPlayer", game.getPlayerName());
        assertEquals(Side.WHITE, game.getPlayerSide());
        assertFalse(game.isGameOver());
        assertNotNull(game.getBoard());
        assertTrue(game.getMoveHistory().isEmpty());
        assertTrue(game.getMoveAnnotations().isEmpty());
    }

    @Test
    void testInitialFen() {
        ChessGameState game = new ChessGameState("game1", "player1", "TestPlayer", Side.WHITE);

        assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", game.getFen());
    }

    @Test
    void testGameOver() {
        ChessGameState game = new ChessGameState("game1", "player1", "TestPlayer", Side.WHITE);

        game.setGameOver(true);
        game.setResult("White wins");

        assertTrue(game.isGameOver());
        assertEquals("White wins", game.getResult());
    }

    @Test
    void testMoveQualityFromEvalLoss() {
        assertEquals(MoveQuality.BEST, MoveQuality.fromEvalLoss(0));
        assertEquals(MoveQuality.BEST, MoveQuality.fromEvalLoss(20));
        assertEquals(MoveQuality.GOOD, MoveQuality.fromEvalLoss(30));
        assertEquals(MoveQuality.INACCURACY, MoveQuality.fromEvalLoss(100));
        assertEquals(MoveQuality.MISTAKE, MoveQuality.fromEvalLoss(200));
        assertEquals(MoveQuality.BLUNDER, MoveQuality.fromEvalLoss(500));
    }

    @Test
    void testMoveAnnotation() {
        MoveAnnotation annotation = new MoveAnnotation("e2e4", 0, -50, MoveQuality.GOOD, false);

        assertEquals("e2e4", annotation.getMove());
        assertEquals(0, annotation.getEvalBefore());
        assertEquals(-50, annotation.getEvalAfter());
        assertEquals(MoveQuality.GOOD, annotation.getQuality());
        assertFalse(annotation.wasMate());
        assertEquals(50, annotation.getEvalSwing());
    }

    @Test
    void testMoveQualityDescriptions() {
        assertEquals("Best move", MoveQuality.BEST.getDescription());
        assertEquals("Blunder", MoveQuality.BLUNDER.getDescription());
        assertEquals("??", MoveQuality.BLUNDER.getEmoji());
    }

    @Test
    void testGetLastMoveAnnotation() {
        ChessGameState game = new ChessGameState("game1", "player1", "TestPlayer", Side.WHITE);

        assertNull(game.getLastMoveAnnotation());

        game.getMoveAnnotations().add(new MoveAnnotation("e2e4", 0, 10, MoveQuality.BEST, false));

        assertNotNull(game.getLastMoveAnnotation());
        assertEquals("e2e4", game.getLastMoveAnnotation().getMove());
    }
}

package com.railway.blindchess.websocket;

import com.google.gson.Gson;
import com.railway.blindchess.model.ChessGameState;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class ChessWebSocketHandler extends TextWebSocketHandler {
    private final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final Gson gson = new Gson();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        System.out.println("WebSocket connected: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        System.out.println("WebSocket disconnected: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        System.out.println("Received message: " + message.getPayload());
    }

    public void broadcastGameState(ChessGameState game) {
        Map<String, Object> update = new HashMap<>();
        update.put("type", "game_update");
        update.put("gameId", game.getGameId());
        update.put("playerName", game.getPlayerName());
        update.put("playerSide", game.getPlayerSide().toString());
        update.put("fen", game.getFen());
        update.put("pgn", game.getPgn());
        update.put("moveHistory", game.getMoveHistory());
        update.put("gameOver", game.isGameOver());
        update.put("result", game.getResult());
        update.put("currentTurn", game.getBoard().getSideToMove().toString());

        String json = gson.toJson(update);
        broadcast(json);
    }

    private void broadcast(String message) {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    System.err.println("Error sending message to session " + session.getId() + ": " + e.getMessage());
                }
            }
        }
    }
}

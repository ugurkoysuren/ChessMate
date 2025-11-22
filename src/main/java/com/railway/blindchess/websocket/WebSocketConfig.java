package com.railway.blindchess.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final ChessWebSocketHandler webSocketHandler;

    public WebSocketConfig(ChessWebSocketHandler webSocketHandler) { this.webSocketHandler = webSocketHandler; }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/ws/chess").setAllowedOrigins("https://chessmate-production-5aae.up.railway.app");
    }
}

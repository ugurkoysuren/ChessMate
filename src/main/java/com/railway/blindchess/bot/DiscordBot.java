package com.railway.blindchess.bot;

import com.github.bhlangonijr.chesslib.Side;
import com.railway.blindchess.chess.ChessGameManager;
import com.railway.blindchess.model.ChessGameState;
import com.railway.blindchess.model.MoveAnnotation;
import com.railway.blindchess.service.OpenRouterService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.List;

@Component
public class DiscordBot extends ListenerAdapter {
    private JDA jda;
    private final ChessGameManager gameManager;
    private final OpenRouterService openRouterService;

    @Value("${discord.bot.token}")
    private String botToken;

    public DiscordBot(ChessGameManager gameManager, OpenRouterService openRouterService) {
        this.gameManager = gameManager;
        this.openRouterService = openRouterService;
    }

    @PostConstruct
    public void init() {
        try {
            jda = JDABuilder.createDefault(botToken)
                    .addEventListeners(this)
                    .build();
            jda.awaitReady();
            System.out.println("Discord bot is ready!");
        } catch (Exception e) {
            System.err.println("Failed to start Discord bot: " + e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }

        Message message = event.getMessage();
        String content = message.getContentRaw();
        MessageChannel channel = event.getChannel();
        String userId = event.getAuthor().getId();
        String userName = event.getAuthor().getName();

        try {
            if (content.startsWith("!chess")) {
                handleChessCommand(content, channel, userId, userName);
            }
        } catch (Exception e) {
            channel.sendMessage("Error: " + e.getMessage()).queue();
        }
    }

    private void handleChessCommand(String content, MessageChannel channel, String userId, String userName) {
        String[] parts = content.trim().split("\\s+");

        if (parts.length < 2) {
            sendHelp(channel);
            return;
        }

        String command = parts[1].toLowerCase();

        switch (command) {
            case "new" -> handleNewGame(parts, channel, userId, userName);
            case "move" -> handleMove(parts, channel, userId);
            case "show" -> handleShow(channel, userId);
            case "legal" -> handleLegalMoves(channel, userId);
            case "resign" -> handleResign(channel, userId);
            case "help" -> sendHelp(channel);
            default -> channel.sendMessage("Unknown command. Use `!chess help` for available commands.").queue();
        }
    }

    private void handleNewGame(String[] parts, MessageChannel channel, String userId, String userName) {
        Side playerSide = Side.WHITE;

        if (parts.length > 2) {
            String color = parts[2].toLowerCase();
            if (color.equals("black") || color.equals("b")) {
                playerSide = Side.BLACK;
            }
        }

        ChessGameState game = gameManager.createGame(userId, userName, playerSide);
        channel.sendMessage(
                "**New game started!** " + userName + " plays as **" + playerSide + "**\n" +
                        "Current position:\n```\n" + getBoardAscii(game) + "\n```\n" +
                        "View the game live at: " + getWebUrl() + "\n" +
                        "Use `!chess move <move>` to make a move (e.g., `!chess move e2e4`)"
        ).queue();
    }

    private void handleMove(String[] parts, MessageChannel channel, String userId) {
        if (parts.length < 3) {
            channel.sendMessage("Please specify a move. Example: `!chess move e2e4`").queue();
            return;
        }

        String move = parts[2];

        try {
            String stockfishMove = gameManager.makePlayerMove(userId, move);
            ChessGameState game = gameManager.getGame(userId);

            MoveAnnotation playerMoveAnnotation = game.getLastMoveAnnotation();
            if (playerMoveAnnotation != null && game.getMoveAnnotations().size() > 1) {
                playerMoveAnnotation = game.getMoveAnnotations().get(game.getMoveAnnotations().size() - 2);
            }

            StringBuilder response = new StringBuilder();
            response.append("**Your move:** ").append(move);

            if (playerMoveAnnotation != null) {
                String qualityEmoji = playerMoveAnnotation.getQualityEmoji();

                if (!qualityEmoji.isEmpty()) {
                    response.append(" ").append(qualityEmoji);
                }

                String currentEvalStr = null;
                if (game.getCurrentEvaluation() != null) {
                    currentEvalStr = String.format("%+.2f", game.getCurrentEvaluation() / 100.0);
                }

                String grokComment = openRouterService.generateMoveResponse(
                        move,
                        playerMoveAnnotation.getQuality(),
                        playerMoveAnnotation.getEvalSwing(),
                        game.getPlayerName(),
                        currentEvalStr
                );

                if (grokComment != null && !grokComment.isEmpty()) {
                    response.append("\n> *").append(grokComment).append("*");
                }
            }
            response.append("\n");

            if (game.isGameOver()) {
                response.append("**Game Over!** ").append(game.getResult()).append("\n```\n")
                        .append(getBoardAscii(game)).append("\n```");
            } else {
                response.append("**Stockfish's move:** ").append(stockfishMove).append("\n");

                if (game.getCurrentEvaluation() != null) {
                    double evalPawns = game.getCurrentEvaluation() / 100.0;
                    String evalStr = String.format("**Evaluation:** %+.2f", evalPawns);
                    if (evalPawns > 0) {
                        evalStr += " (White is better)";
                    } else if (evalPawns < 0) {
                        evalStr += " (Black is better)";
                    } else {
                        evalStr += " (Equal)";
                    }
                    response.append(evalStr).append("\n");
                }

                response.append("```\n").append(getBoardAscii(game)).append("\n```");
            }

            channel.sendMessage(response.toString()).queue();
        } catch (Exception e) {
            channel.sendMessage("Error making move: " + e.getMessage()).queue();
        }
    }

    private void handleShow(MessageChannel channel, String userId) {
        ChessGameState game = gameManager.getGame(userId);
        if (game == null) {
            channel.sendMessage("No active game. Start a new game with `!chess new`").queue();
            return;
        }

        channel.sendMessage(
                "**Current position** (You play as " + game.getPlayerSide() + "):\n" +
                        "```\n" + getBoardAscii(game) + "\n```\n" +
                        "Moves: " + game.getPgn()
        ).queue();
    }

    private void handleLegalMoves(MessageChannel channel, String userId) {
        ChessGameState game = gameManager.getGame(userId);
        if (game == null) {
            channel.sendMessage("No active game. Start a new game with `!chess new`").queue();
            return;
        }

        List<String> legalMoves = gameManager.getLegalMoves(userId);
        channel.sendMessage("**Legal moves:** " + String.join(", ", legalMoves)).queue();
    }

    private void handleResign(MessageChannel channel, String userId) {
        ChessGameState game = gameManager.getGame(userId);
        if (game == null) {
            channel.sendMessage("No active game.").queue();
            return;
        }

        gameManager.resignGame(userId);
        channel.sendMessage("You resigned. Stockfish wins!").queue();
    }

    private void sendHelp(MessageChannel channel) {
        String help = """
                **Blind Chess Bot Commands:**
                `!chess new [white|black]` - Start a new game (default: white)
                `!chess move <move>` - Make a move (UCI notation, e.g., e2e4, g1f3)
                `!chess show` - Show current board position
                `!chess legal` - Show all legal moves
                `!chess resign` - Resign the game
                `!chess help` - Show this help message
                
                **Example:** `!chess new white` then `!chess move e2e4`
                Watch the game live on the web frontend!
                """;
        channel.sendMessage(help).queue();
    }

    private String getBoardAscii(ChessGameState game) {
        StringBuilder sb = new StringBuilder();
        String fen = game.getFen().split(" ")[0];
        String[] ranks = fen.split("/");

        sb.append("  a b c d e f g h\n");
        for (int i = 0; i < ranks.length; i++) {
            sb.append(8 - i).append(" ");
            String rank = ranks[i];
            for (char c : rank.toCharArray()) {
                if (Character.isDigit(c)) {
                    int spaces = c - '0';
                    sb.append(". ".repeat(Math.max(0, spaces)));
                } else {
                    sb.append(c).append(" ");
                }
            }
            sb.append(8 - i).append("\n");
        }
        sb.append("  a b c d e f g h");

        return sb.toString();
    }

    private String getWebUrl() {
        String railwayUrl = System.getenv("RAILWAY_PUBLIC_DOMAIN");
        if (railwayUrl != null) {
            return "https://" + railwayUrl;
        }
        return "http://localhost:8080";
    }
}

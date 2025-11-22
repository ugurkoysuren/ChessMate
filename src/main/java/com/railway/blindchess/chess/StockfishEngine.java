package com.railway.blindchess.chess;

import org.springframework.stereotype.Component;

import java.io.*;

@Component
public class StockfishEngine {
    private Process stockfishProcess;
    private BufferedReader reader;
    private BufferedWriter writer;
    private static final String STOCKFISH_PATH = System.getenv().getOrDefault("STOCKFISH_PATH", "/usr/games/stockfish");

    public void start() throws IOException {
        stockfishProcess = Runtime.getRuntime().exec(STOCKFISH_PATH);
        reader = new BufferedReader(new InputStreamReader(stockfishProcess.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(stockfishProcess.getOutputStream()));

        sendCommand("uci");
        String line;
        while ((line = readLine()) != null) {
            if (line.equals("uciok")) break;
        }
        sendCommand("setoption name Skill Level value 10");
        sendCommand("isready");
        while ((line = readLine()) != null) {
            if (line.equals("readyok")) break;
        }
    }

    public String getBestMove(String fen, int thinkTimeMs) throws IOException {
        sendCommand("position fen " + fen);
        sendCommand("go movetime " + thinkTimeMs);

        String bestMove = null;
        String line;
        while ((line = readLine()) != null) {
            if (line.startsWith("bestmove")) {
                String[] parts = line.split(" ");
                if (parts.length > 1) bestMove = parts[1];
                break;
            }
        }
        return bestMove;
    }

    public EvaluationResult getBestMoveWithEvaluation(String fen, int thinkTimeMs) throws IOException {
        sendCommand("position fen " + fen);
        sendCommand("go movetime " + thinkTimeMs);

        String bestMove = null;
        Integer evaluation = null;
        String line;

        while ((line = readLine()) != null) {
            if (line.startsWith("info") && line.contains("score")) {
                if (line.contains("score cp")) {
                    String[] parts = line.split("\\s+");
                    for (int i = 0; i < parts.length - 1; i++) {
                        if (parts[i].equals("cp")) {
                            try {
                                evaluation = Integer.parseInt(parts[i + 1]);
                            } catch (NumberFormatException ignored) {
                            }
                            break;
                        }
                    }
                } else if (line.contains("score mate")) {
                    String[] parts = line.split("\\s+");
                    for (int i = 0; i < parts.length - 1; i++) {
                        if (parts[i].equals("mate")) break;
                    }
                }
            }

            if (line.startsWith("bestmove")) {
                String[] parts = line.split(" ");
                if (parts.length > 1) bestMove = parts[1];
                break;
            }
        }

        return new EvaluationResult(bestMove, evaluation);
    }

    public static class EvaluationResult {
        private final String bestMove;
        private final Integer centipawns;

        public EvaluationResult(String bestMove, Integer centipawns) {
            this.bestMove = bestMove;
            this.centipawns = centipawns;
        }

        public String getBestMove() {
            return bestMove;
        }

        public Integer getCentipawns() {
            return centipawns;
        }
    }

    private void sendCommand(String command) throws IOException {
        writer.write(command + "\n");
        writer.flush();
    }

    private String readLine() throws IOException {
        return reader.readLine();
    }

    public void stop() throws IOException {
        if (stockfishProcess != null) {
            sendCommand("quit");
            stockfishProcess.destroy();
        }
    }
}

package com.railway.blindchess.model;

public class MoveAnnotation {
    private final String move;
    private final Integer evalBefore;
    private final Integer evalAfter;
    private final MoveQuality quality;
    private final boolean wasMate;

    public MoveAnnotation(String move, Integer evalBefore, Integer evalAfter, MoveQuality quality, boolean wasMate) {
        this.move = move;
        this.evalBefore = evalBefore;
        this.evalAfter = evalAfter;
        this.quality = quality;
        this.wasMate = wasMate;
    }

    public String getMove() {
        return move;
    }

    public Integer getEvalBefore() {
        return evalBefore;
    }

    public Integer getEvalAfter() {
        return evalAfter;
    }

    public MoveQuality getQuality() {
        return quality;
    }

    public boolean wasMate() {
        return wasMate;
    }

    public Integer getEvalSwing() {
        return (evalBefore != null && evalAfter != null) ? Math.abs(evalAfter - evalBefore) : null;
    }

    public String getQualityEmoji() {
        return wasMate ? "checkmate" : (quality != null ? quality.getEmoji() : "");
    }

    public String getQualityDescription() {
        return wasMate ? "Checkmate!" : (quality != null ? quality.getDescription() : "");
    }
}

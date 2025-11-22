package com.railway.blindchess.model;

public enum MoveQuality {
    BEST("best", "Best move", 0, 25),
    GOOD("", "Good", 25, 50),
    INACCURACY("?!", "Inaccuracy", 50, 150),
    MISTAKE("?", "Mistake", 150, 300),
    BLUNDER("??", "Blunder", 300, Integer.MAX_VALUE);

    private final String emoji;
    private final String description;
    private final int minLoss;
    private final int maxLoss;

    MoveQuality(String emoji, String description, int minLoss, int maxLoss) {
        this.emoji = emoji;
        this.description = description;
        this.minLoss = minLoss;
        this.maxLoss = maxLoss;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getDescription() {
        return description;
    }

    public static MoveQuality fromEvalLoss(int centipawnLoss) {
        int absLoss = Math.abs(centipawnLoss);
        for (MoveQuality quality : values()) {
            if (absLoss >= quality.minLoss && absLoss < quality.maxLoss) return quality;
        }
        return GOOD;
    }
}

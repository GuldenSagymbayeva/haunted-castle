package hauntedcastle.game;

/** Scales how hard monsters hit, how often they strike as you flee, and the final score. */
public enum Difficulty {
    EASY("Easy", "monsters hit softer and rarely catch you fleeing", 0.7, 0.3, 0.75),
    NORMAL("Normal", "the intended challenge", 1.0, 0.5, 1.0),
    HARD("Hard", "monsters hit harder and often catch you fleeing", 1.15, 0.7, 1.5);

    private final String label;
    private final String blurb;
    private final double damageTaken;
    private final double partingStrikeChance;
    private final double scoreMultiplier;

    Difficulty(String label, String blurb, double damageTaken, double partingStrikeChance, double scoreMultiplier) {
        this.label = label;
        this.blurb = blurb;
        this.damageTaken = damageTaken;
        this.partingStrikeChance = partingStrikeChance;
        this.scoreMultiplier = scoreMultiplier;
    }

    public String label() {
        return label;
    }

    public String blurb() {
        return blurb;
    }

    /** Damage a monster deals to the player after scaling; never below 1 for a landed blow. */
    public int scaleDamageTaken(int damage) {
        return damage <= 0 ? 0 : Math.max(1, (int) Math.round(damage * damageTaken));
    }

    public double partingStrikeChance() {
        return partingStrikeChance;
    }

    public int scaleScore(int score) {
        return (int) Math.round(score * scoreMultiplier);
    }
}

package com.starfall.game;

/** Hand-authored campaign pacing, with a command ship every third operation. */
public final class Campaign {
    public static final int CASUAL = 0, STANDARD = 1, VETERAN = 2, NIGHTMARE = 3;
    public static final String[] DIFFICULTIES = {"CASUAL", "STANDARD", "VETERAN", "NIGHTMARE"};
    public static final int STAGES = 12;
    public static final String[] NAMES = {
        "DEAD SIGNAL", "DEBRIS FIELD", "THE WARDEN", "BLACKOUT",
        "KILL CORRIDOR", "REVENANT", "ASH FRONT", "IRON VEIL",
        "DOMINION", "NO RETURN", "EVENT HORIZON", "SINGULARITY"
    };
    public static final String[] BRIEFS = {
        "Break through the outer patrol.", "Armored gunships. Watch the flanks.",
        "Neutralize the blockade commander.", "Interceptors are closing fast.",
        "Overlapping fire. Keep moving.", "Destroy the twin-turret carrier.",
        "Heavy resistance across the front.", "Pierce the final defense line.",
        "Eliminate the fleet command ship.", "No support beyond this point.",
        "Survive the enemy counterattack.", "End the war. Destroy the core."
    };
    private Campaign() {}
    public static boolean boss(int stage) { return stage % 3 == 0; }
    public static String name(int stage) { return NAMES[Math.max(0, Math.min(STAGES-1, stage-1))]; }
    public static float duration(int stage) { return 22 + stage * .9f; }
    public static float formationInterval(int stage, int difficulty) {
        if (difficulty == CASUAL) return Math.max(1.8f, 2.65f-stage*.05f);
        if (difficulty == STANDARD) return Math.max(1.2f, 2.05f-stage*.045f);
        return Math.max(.72f, 1.45f-stage*.045f-(difficulty-VETERAN)*.18f);
    }
    public static float bulletSpeed(int stage, int difficulty) {
        if (difficulty == CASUAL) return 120+stage*6;
        if (difficulty == STANDARD) return 155+stage*7;
        return 190+stage*9+(difficulty-VETERAN)*32;
    }
    public static int hull(int difficulty) { return difficulty == CASUAL ? 5 : difficulty == STANDARD ? 4 : 3; }
    public static float durability(int difficulty) { return difficulty == CASUAL ? .65f : difficulty == STANDARD ? .82f : difficulty == VETERAN ? 1 : 1.25f; }
    public static float firingDelay(int difficulty) { return difficulty == CASUAL ? 1.6f : difficulty == STANDARD ? 1.25f : difficulty == VETERAN ? 1 : .9f; }
    public static float damageGrace(int difficulty) { return difficulty == CASUAL ? 1.8f : difficulty == STANDARD ? 1.4f : 1.1f; }
    public static float dashSeconds(int difficulty) { return difficulty == CASUAL ? 4.5f : difficulty == STANDARD ? 5.5f : 6.5f; }
    public static float novaSeconds(int difficulty) { return difficulty == CASUAL ? 26 : difficulty == STANDARD ? 34 : 42; }
}

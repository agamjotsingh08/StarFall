package com.starfall.game;

/** All three ships are available immediately; no purchases or unlock grind. */
public final class Fleet {
    public static final int VANGUARD = 0, FALCON = 1, BASTION = 2, COUNT = 3;
    public static final String[] NAMES = {"VANGUARD", "FALCON", "BASTION"};
    public static final String[] ROLES = {"BALANCED / NOVA SPECIALIST", "INTERCEPTOR / SPEED SPECIALIST", "ARMORED / SURVIVAL SPECIALIST"};
    public static final String[] PERKS = {"Nova recharges 20% faster.", "20% faster cannons. 25% faster dash. One less hull.", "One extra hull. Collect supplies from farther away."};
    private Fleet() {}
    public static int valid(int ship) { return Math.max(0, Math.min(COUNT-1, ship)); }
    public static int hullBonus(int ship) { return ship == BASTION ? 1 : ship == FALCON ? -1 : 0; }
    public static float fireRate(int ship) { return ship == FALCON ? 1.2f : 1; }
    public static float dashMultiplier(int ship) { return ship == FALCON ? .75f : 1; }
    public static float novaMultiplier(int ship) { return ship == VANGUARD ? .8f : 1; }
    public static float magnetRadius(int ship) { return ship == BASTION ? 155 : 105; }
}

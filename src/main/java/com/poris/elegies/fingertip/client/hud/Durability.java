package com.poris.elegies.fingertip.client.hud;

/** The durability percent's arithmetic (DurabilityPercent), apart from the decorator so plain unit tests reach it. */
final class Durability {
    /** Whole percent left, rounded down, but 1 rather than 0 while a use is left. */
    static int percent(int damage, int maxDamage) {
        int left = maxDamage - damage;
        return left <= 0 ? 0 : Math.max(1, (int)((long)left * 100 / maxDamage));
    }
    private Durability() {}
}

package com.poris.elegies.fingertip.api.inventory;

import java.util.Objects;

/** A location in a player's storage. This is a hint, never an item identity or a menu slot id. */
public record SourceAddress(String provider, int slot) implements Comparable<SourceAddress> {
    private static final java.util.regex.Pattern ID = java.util.regex.Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");
    public static final String VANILLA = "minecraft:inventory";
    public static final int EXTERNAL = -3;
    public SourceAddress {
        Objects.requireNonNull(provider);
        if (provider.length() > 256 || !ID.matcher(provider).matches() || slot < 0 || slot >= 65536)
            throw new IllegalArgumentException("Invalid inventory source address");
    }
    /** Vanilla addresses never change; screens ask for them every frame, so reuse them instead of re-validating. */
    private static final SourceAddress[] VANILLA_SLOTS = new SourceAddress[64];
    static { for (int slot = 0; slot < VANILLA_SLOTS.length; slot++) VANILLA_SLOTS[slot] = new SourceAddress(VANILLA, slot); }
    public static SourceAddress vanilla(int slot) {
        return slot >= 0 && slot < VANILLA_SLOTS.length ? VANILLA_SLOTS[slot] : new SourceAddress(VANILLA, slot);
    }
    public static SourceAddress legacy(int slot) { return slot < 0 ? null : vanilla(slot); }
    public boolean vanilla() { return VANILLA.equals(provider); }
    /** Only for vanilla packet/mixin boundaries. External slots must never index Inventory.items. */
    public int legacySlot() { return vanilla() ? slot : EXTERNAL; }
    @Override public int compareTo(SourceAddress other) {
        if (vanilla() != other.vanilla()) return vanilla() ? -1 : 1;
        int order = provider.compareTo(other.provider);
        return order == 0 ? Integer.compare(slot, other.slot) : order;
    }
}

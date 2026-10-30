package com.poris.elegies.fingertip.client.input;

import java.util.List;
import java.util.Optional;

/** Stable client preference IDs, independent of whether the owning mod is currently installed. */
public record WheelActionRef(Kind kind, String target) {
    public enum Kind { NATIVE, KEY, ACTION }
    public static final List<String> NATIVE_IDS = List.of("inventory", "chat", "swap", "perspective",
            "editor", "hud", "drop", "drop_stack", "pick", "screenshot", "none");

    public static Optional<WheelActionRef> parse(String stored) {
        if (stored == null) return Optional.empty();
        if (NATIVE_IDS.contains(stored)) return Optional.of(new WheelActionRef(Kind.NATIVE, stored));
        if (stored.startsWith("key:") && validKey(stored.substring(4)))
            return Optional.of(new WheelActionRef(Kind.KEY, stored.substring(4)));
        if (stored.startsWith("action:") && validAction(stored.substring(7)))
            return Optional.of(new WheelActionRef(Kind.ACTION, stored.substring(7)));
        return Optional.empty();
    }
    public static String key(String name) {
        if (!validKey(name)) throw new IllegalArgumentException("Invalid wheel key name");
        return "key:" + name;
    }
    public static String action(String id) {
        if (!validAction(id)) throw new IllegalArgumentException("Invalid wheel action ID");
        return "action:" + id;
    }
    /** ActionCatalog uses mapping:<translation key> for key mappings and resource IDs for API actions. */
    public static String fromCatalogId(String id) {
        if (id == null) throw new IllegalArgumentException("Missing action catalog ID");
        return id.startsWith("mapping:") ? key(id.substring(8)) : action(id);
    }
    private static boolean validKey(String name) {
        return name != null && !name.isBlank() && name.length() <= 512
                && name.codePoints().noneMatch(Character::isISOControl);
    }
    private static boolean validAction(String id) {
        return id != null && id.length() <= 512 && id.matches("[a-z0-9_.-]+:[a-z0-9/._-]+");
    }
}

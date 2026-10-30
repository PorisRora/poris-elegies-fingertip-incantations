package com.poris.elegies.fingertip.core;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;

/**
 * Key mappings expose translation keys, not mod ownership. Only accept a matching namespace token. The editor's catalog
 * asks this for every key against every mod, so names are normalized once and by hand: with String.replaceAll (which
 * compiles its pattern on each call) a 270-key, 170-mod pack took about 100 ms per catalog.
 */
public final class ActionOwners {
    private static final Set<String> VANILLA = Set.of("attack", "use", "forward", "left", "back", "right",
            "jump", "sneak", "sprint", "drop", "inventory", "chat", "playerlist", "pickItem", "command",
            "socialInteractions", "screenshot", "togglePerspective", "smoothCamera", "fullscreen",
            "spectatorOutlines", "swapOffhand", "advancements", "saveToolbarActivator", "loadToolbarActivator");

    public static String resolve(String name, String category, Collection<String> modIds) {
        String nameText = "." + normalize(name) + ".", categoryText = "." + normalize(category) + ".";
        // The longest matching id wins; equal lengths go to the alphabetically first.
        String owner = null;
        for (String id : modIds) {
            if (id.equals("minecraft") || id.equals("neoforge")) continue;
            String token = "." + normalize(id) + ".";
            if (!nameText.contains(token) && !categoryText.contains(token)) continue;
            if (owner == null || id.length() > owner.length() || id.length() == owner.length() && id.compareTo(owner) < 0) owner = id;
        }
        if (owner != null) return owner;
        if (name.startsWith("key.") && (VANILLA.contains(name.substring(4)) || name.matches("key\\.hotbar\\.[1-9]")))
            return "minecraft";
        // Do not mislabel unknown third-party keys as Minecraft just because they reuse a vanilla category.
        return "category:" + category;
    }

    /** Lower case, with every character other than a–z and 0–9 turned into a dot. */
    private static String normalize(String value) {
        char[] text = value.toLowerCase(Locale.ROOT).toCharArray();
        for (int i = 0; i < text.length; i++) {
            char c = text[i];
            if (!(c >= 'a' && c <= 'z' || c >= '0' && c <= '9')) text[i] = '.';
        }
        return new String(text);
    }
    private ActionOwners() {}
}

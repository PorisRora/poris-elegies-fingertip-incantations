package com.poris.elegies.fingertip.client.input;

import com.poris.elegies.fingertip.client.input.KeyState;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import com.poris.elegies.fingertip.Fingertip;
import com.poris.elegies.fingertip.client.hud.HudLayout;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/** Persistent HUD shortcuts have one owner: layout.json. KeyMapping remains the runtime adapter. */
public final class ShortcutConfiguration {
    private static boolean loaded;

    /**
     * A new layout starts from the registered default keys. The one-time import of keys that early previews kept in
     * options.txt was removed in preview19.23 (2026-10-28): every layout since then stores its own.
     */
    public static void initialize() {
        if (loaded || Minecraft.getInstance().options == null) return;
        var keys = HudLayout.INSTANCE.shortcuts;
        if (keys == null) keys = current();
        for (int id = 0; id < Fingertip.SLOT_COUNT; id++) {
            ShortcutKeyRules.clear(id);
            var value = keys.get(Integer.toString(id));
            if (value == null) continue;
            try {
                if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw new IllegalArgumentException("Expected key string");
                String[] parts = value.getAsString().split(":", -1);
                if (parts.length > 2) throw new IllegalArgumentException("Invalid key combination");
                var modifier = parts.length == 2 ? KeyModifier.valueOf(parts[1]) : KeyModifier.NONE;
                KeyState.bind(ShortcutKeys.SLOTS[id], modifier, InputConstants.getKey(parts[0]));
            } catch (RuntimeException error) {
                Fingertip.LOGGER.warn("Cleared malformed HUD shortcut {}", id + 1);
            }
        }
        loaded = true;
        KeyMapping.resetMapping();
        ShortcutKeyRules.repair();
        save();
    }

    /** Reapply an explicitly loaded configuration; ordinary ticks never overwrite live edits. */
    public static void reload() { InputRouter.releaseAll(); loaded = false; initialize(); }

    private static JsonObject current() {
        var keys = new JsonObject();
        for (int id = 0; id < Fingertip.SLOT_COUNT; id++) {
            var key = ShortcutKeys.SLOTS[id];
            keys.addProperty(Integer.toString(id), key.saveString()
                    + (KeyState.modifier(key) == KeyModifier.NONE ? "" : ":" + KeyState.modifier(key).name()));
        }
        return keys;
    }

    public static void save() {
        if (!loaded) initialize();
        HudLayout.INSTANCE.shortcuts = current();
        HudLayout.INSTANCE.save();
    }

    private ShortcutConfiguration() {}
}

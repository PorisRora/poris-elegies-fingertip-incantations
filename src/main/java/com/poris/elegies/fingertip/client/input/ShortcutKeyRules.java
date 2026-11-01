package com.poris.elegies.fingertip.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.poris.elegies.fingertip.Fingertip;
import com.poris.elegies.fingertip.mixin.client.KeyMappingAccess;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;
import java.util.Objects;
import java.util.function.IntFunction;

/** Shared validation for capture, our saved shortcuts and dispatch; only our shortcuts may be repaired. */
public final class ShortcutKeyRules {
    private static int repairedBeforeWorld;

    /** A value only: constructing a preview must never register a Minecraft KeyMapping. */
    public record KeyChoice(InputConstants.Key key, KeyModifier modifier) {
        public KeyChoice {
            Objects.requireNonNull(key);
            Objects.requireNonNull(modifier);
            if (key.equals(InputConstants.UNKNOWN)) modifier = KeyModifier.NONE;
        }
        public boolean isUnbound() { return key.equals(InputConstants.UNKNOWN); }
    }

    public static KeyChoice current(int slot) {
        var mapping = ShortcutKeys.SLOTS[slot];
        return new KeyChoice(mapping.getKey(), mapping.getKeyModifier());
    }

    public static Component problem(int slot, InputConstants.Key key, KeyModifier modifier) {
        return problem(slot, key, modifier, ShortcutKeyRules::current);
    }

    /** Checks the proposed final set, including hidden/paused slots, without mutating live keys. */
    public static Component problem(int slot, InputConstants.Key key, KeyModifier modifier,
                                    IntFunction<KeyChoice> choices) {
        if (HotbarControl.nativeSlot(slot)) return Component.translatable("poris_elegies_fingertip_incantations.native_slot_locked");
        if (HotbarControl.preservesKey(key)) return Component.translatable("poris_elegies_fingertip_incantations.native_key_paused");
        return problem(slot, key, modifier, ShortcutKeys.SLOTS.length, choices);
    }

    /** What is wrong with a key for a slot; worded once for key capture and once for a paused slot's hint. */
    private record Issue(String kind, Object detail) {}

    // The whitelist comes last, so key capture (which accepts it) still sees every other problem first.
    private static Issue issue(int slot, InputConstants.Key key, KeyModifier modifier, int limit, IntFunction<KeyChoice> choices) {
        if (key.equals(InputConstants.UNKNOWN)) return null;
        if (!validCode(key)) return new Issue("invalid", null);
        if (NativeInputPolicy.reservedBinding(key) || KeyModifier.isKeyCodeModifier(key)) return new Issue("reserved", null);
        for (int i = 0; i < limit; i++) {
            var other = choices.apply(i);
            if (i != slot && sameCombination(key, modifier, other)) return new Issue("occupied", i + 1);
        }
        var nativeMod = ModKeyWhitelist.reservedBy(key);
        if (nativeMod != null && !OptionalKeyPolicy.optional(key)) return new Issue("whitelist", Component.translatable(nativeMod.getName()));
        return null;
    }

    /**
     * A key a whitelisted mod also uses is accepted: the slot is paused while that mod stays whitelisted, the same as
     * when the mod takes the key later (2026-10-26).
     */
    private static Component problem(int slot, InputConstants.Key key, KeyModifier modifier, int limit,
                                     IntFunction<KeyChoice> choices) {
        var issue = issue(slot, key, modifier, limit, choices);
        if (issue == null || issue.kind().equals("whitelist")) return null;
        return switch (issue.kind()) {
            case "invalid" -> Component.translatable("poris_elegies_fingertip_incantations.invalid_key");
            case "reserved" -> Component.translatable("poris_elegies_fingertip_incantations.reserved_key");
            default -> Component.translatable("poris_elegies_fingertip_incantations.key_occupied", issue.detail());
        };
    }

    private static Component pauseReason(Issue issue) {
        return switch (issue.kind()) {
            case "reserved" -> Component.translatable("poris_elegies_fingertip_incantations.paused_reserved");
            case "whitelist" -> Component.translatable("poris_elegies_fingertip_incantations.paused_whitelist", issue.detail());
            default -> Component.translatable("poris_elegies_fingertip_incantations.paused_occupied", issue.detail());
        };
    }

    public static boolean sameCombination(InputConstants.Key key, KeyModifier modifier, KeyMapping other) {
        return !key.equals(InputConstants.UNKNOWN) && key.equals(other.getKey()) && modifier == other.getKeyModifier();
    }

    public static boolean sameCombination(InputConstants.Key key, KeyModifier modifier, KeyChoice other) {
        return !key.equals(InputConstants.UNKNOWN) && key.equals(other.key()) && modifier == other.modifier();
    }

    // GLFW key symbols are sparse. A numeric key name in options.txt need not be a real key.
    private static boolean validCode(InputConstants.Key key) {
        int code = key.getValue();
        if (key.getType() == InputConstants.Type.MOUSE) return code >= 0 && code <= GLFW.GLFW_MOUSE_BUTTON_LAST;
        if (key.getType() == InputConstants.Type.SCANCODE) return code >= 0;
        return code == 32 || code == 39 || code >= 44 && code <= 57 || code == 59 || code == 61
                || code >= 65 && code <= 96 || code >= 161 && code <= 162 || code >= 256 && code <= 269
                || code >= 280 && code <= 284 || code >= 290 && code <= 314 || code >= 320 && code <= 336
                || code >= 340 && code <= 348;
    }

    // Each slot's standing is worked out only when something it depends on changes (2026-10-26):
    // KeyMapping.resetMapping (every rebind, ours or vanilla's Controls), a screen opening or closing, joining a world,
    // and our own input settings call changed(). A slot whose saved key now conflicts (core key, a whitelisted mod's
    // key, an earlier slot's key) is paused, not cleared: it keeps the key and owns it again once the conflict is gone.
    private static boolean dirty = true;
    private static final boolean[] OWNER = new boolean[ShortcutKeys.SLOTS.length];
    private static final Component[] CONFLICT = new Component[ShortcutKeys.SLOTS.length];
    static int recomputed;   // read by the smoke probe

    public static void changed() { dirty = true; }

    private static void update() {
        if (!dirty || Minecraft.getInstance().options == null) return;
        int cleared = 0;
        for (int id = 0; id < ShortcutKeys.SLOTS.length; id++) {
            var mapping = ShortcutKeys.SLOTS[id];
            if (mapping.isUnbound() || validCode(mapping.getKey())) continue;
            // An unreadable key (a damaged or foreign options file) cannot come back by itself, so it is removed.
            Fingertip.LOGGER.warn("Cleared invalid HUD shortcut {} ({})", id + 1, mapping.getKey().getName());
            clear(id); cleared++;
        }
        if (cleared > 0) { KeyMapping.resetMapping(); ShortcutConfiguration.save(); repairedBeforeWorld += cleared; }
        for (int id = 0; id < ShortcutKeys.SLOTS.length; id++) {
            var mapping = ShortcutKeys.SLOTS[id];
            boolean bound = !HotbarControl.nativeSlot(id) && !mapping.isUnbound();
            var issue = bound ? issue(id, mapping.getKey(), mapping.getKeyModifier(), id, ShortcutKeyRules::current) : null;
            CONFLICT[id] = issue == null ? null : pauseReason(issue);
            OWNER[id] = bound && CONFLICT[id] == null && !HotbarControl.preservesKey(mapping.getKey())
                    && !OptionalKeyPolicy.paused(mapping.getKey());
        }
        dirty = false; recomputed++;
    }

    /** Lowest stable slot ID wins; a conflicting or paused slot owns nothing. */
    public static boolean validOwner(int id) {
        update();
        return OWNER[id];
    }

    /** Why this slot's saved key is paused by a conflict (core key, whitelisted mod key, earlier slot), or null. */
    public static Component conflict(int id) {
        update();
        return id >= 0 && id < CONFLICT.length ? CONFLICT[id] : null;
    }

    public static void clear(int id) {
        var mapping = ShortcutKeys.SLOTS[id];
        mapping.setDown(false);
        ((KeyMappingAccess)mapping).fingertip$clickCount(0);
        mapping.setKeyModifierAndCode(KeyModifier.NONE, InputConstants.UNKNOWN);
    }

    /**
     * Runs each tick before any HUD action is dispatched, but only recomputes after {@link #changed()}; then it also
     * removes unreadable keys and reports them once a player is in a world.
     */
    public static void repair() {
        var mc = Minecraft.getInstance();
        if (mc.options == null) return;
        update();
        if (repairedBeforeWorld > 0 && mc.player != null) {
            mc.player.displayClientMessage(Component.translatable("poris_elegies_fingertip_incantations.keys_repaired", repairedBeforeWorld), false);
            repairedBeforeWorld = 0;
        }
    }

    private ShortcutKeyRules() {}
}

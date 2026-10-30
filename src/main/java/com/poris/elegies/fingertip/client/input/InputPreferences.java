package com.poris.elegies.fingertip.client.input;

import com.google.gson.annotations.SerializedName;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;

/** Client preferences only. No item identities, inventory contents or server authority live here. */
public final class InputPreferences {
    public static final int WHEEL_SLOTS = 10, INVENTORY_SLOT = 0, EDITOR_SLOT = 5;
    public boolean functionKeys;
    public int functionMask = 0x1ff;
    public boolean takeQ, takeE, takeT;
    /** On for a new install (2026-10-28, user: "我们轮盘默认打开吧"); a saved setting keeps what the player chose. */
    public boolean wheelEnabled = true;
    /**
     * Drag (default): hold the middle button, release over an action to run it once, in the centre to cancel. Off: the
     * click wheel of 0.1-beta, where drop can be clicked again and again. A layout saved before it has no such field and reads as drag.
     */
    public boolean wheelDrag = true;
    /**
     * "Use real slot" (使用真实格): right-clicking an item held from storage shows other mods its real slot (2026-10-27,
     * user; named on 10-28). Saved under its first name, so a player's choice survives the rename. Off if one breaks.
     */
    @SerializedName("hotbarCompat") public boolean useRealSlot = true;
    public List<String> wheel = defaults();
    public static final List<String> ACTIONS = WheelActionRef.NATIVE_IDS;

    public static List<String> defaults() {
        return new ArrayList<>(List.of("inventory", "chat", "screenshot", "swap", "perspective", "editor", "hud", "drop_stack", "drop", "pick"));
    }
    public static boolean optional(int key) { return functionKey(key) || key == GLFW.GLFW_KEY_Q || key == GLFW.GLFW_KEY_E || key == GLFW.GLFW_KEY_T; }
    public boolean claims(int key) {
        if (functionKey(key)) return functionKeys && (functionMask & 1 << (key - GLFW.GLFW_KEY_F1)) != 0;
        return key == GLFW.GLFW_KEY_Q && takeQ || key == GLFW.GLFW_KEY_E && takeE || key == GLFW.GLFW_KEY_T && takeT;
    }
    /** F1 to F9, one bit each in functionMask. */
    private static boolean functionKey(int key) { return key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F9; }
    public void normalize() {
        functionMask &= 0x1ff;
        var normalized = defaults();
        if (wheel != null) for (int i = 0; i < Math.min(WHEEL_SLOTS, wheel.size()); i++)
            if (WheelActionRef.parse(wheel.get(i)).isPresent()) normalized.set(i, wheel.get(i));
        // These two spatial anchors are invariant, including with all optional takeover switches off.
        // Preserve displaced custom actions in the action's old location or a free/duplicate sector.
        pin(normalized, EDITOR_SLOT, "editor");
        pin(normalized, INVENTORY_SLOT, "inventory");
        if (takeE) wheelEnabled = true;
        wheel = normalized;
    }
    public boolean lockedWheelSlot(int index) {
        return index == INVENTORY_SLOT || index == EDITOR_SLOT;
    }
    private static void pin(List<String> values, int index, String action) {
        String displaced = values.get(index);
        if (action.equals(displaced)) return;
        int previous = values.indexOf(action);
        values.set(index, action);
        if (previous >= 0 && previous != index) {
            values.set(previous, displaced);
            return;
        }
        if ("none".equals(displaced) || values.contains(displaced)) return;
        int receiver = -1;
        for (int i = 1; i < WHEEL_SLOTS; i++) {
            if (i == EDITOR_SLOT) continue;
            if ("none".equals(values.get(i))) { receiver = i; break; }
            if (receiver < 0 && java.util.Collections.frequency(values, values.get(i)) > 1) receiver = i;
        }
        if (receiver >= 0) values.set(receiver, displaced);
    }
}

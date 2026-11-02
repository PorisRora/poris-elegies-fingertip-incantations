package com.poris.elegies.fingertip.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.poris.elegies.fingertip.Fingertip;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class ShortcutKeys {
    public static final String PREFIX = "key." + Fingertip.MOD_ID + ".";
    public static final String CATEGORY = "key.categories." + Fingertip.MOD_ID;
    public static final KeyMapping OPEN = key("open", GLFW.GLFW_KEY_UNKNOWN);
    /** Slots 1 to 9 start on the number keys 1 to 9; the rest start unbound. */
    public static final KeyMapping[] SLOTS = new KeyMapping[Fingertip.SLOT_COUNT];
    static { for (int i = 0; i < SLOTS.length; i++) SLOTS[i] = key("slot." + (i + 1), i < 9 ? GLFW.GLFW_KEY_1 + i : GLFW.GLFW_KEY_UNKNOWN); }
    private static KeyMapping key(String id, int code) {
        return new KeyMapping(PREFIX + id, InputConstants.Type.KEYSYM, code, CATEGORY);
    }
    public static void register() {
        net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(OPEN);
    }
    public static int index(KeyMapping mapping) {
        for (int i = 0; i < SLOTS.length; i++) if (SLOTS[i] == mapping) return i;
        return -1;
    }
    private ShortcutKeys() {}
}

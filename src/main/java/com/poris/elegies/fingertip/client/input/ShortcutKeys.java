package com.poris.elegies.fingertip.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.poris.elegies.fingertip.Fingertip;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Fingertip.MOD_ID, value = Dist.CLIENT)
public final class ShortcutKeys {
    public static final String PREFIX = "key." + Fingertip.MOD_ID + ".";
    public static final String CATEGORY = "key.categories." + Fingertip.MOD_ID;
    public static final KeyMapping OPEN = key("open", GLFW.GLFW_KEY_UNKNOWN);
    /** Slots 1 to 9 start on the number keys 1 to 9; the rest start unbound. */
    public static final KeyMapping[] SLOTS = new KeyMapping[Fingertip.SLOT_COUNT];
    static { for (int i = 0; i < SLOTS.length; i++) SLOTS[i] = key("slot." + (i + 1), i < 9 ? GLFW.GLFW_KEY_1 + i : GLFW.GLFW_KEY_UNKNOWN); }
    private static KeyMapping key(String id, int code) {
        return new KeyMapping(PREFIX + id, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, code, CATEGORY);
    }
    @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) {
        // Constructor registration retains runtime key lookup; HUD slots have their own editor/storage.
        event.register(OPEN);
    }
    public static int index(KeyMapping mapping) {
        for (int i = 0; i < SLOTS.length; i++) if (SLOTS[i] == mapping) return i;
        return -1;
    }
    private ShortcutKeys() {}
}

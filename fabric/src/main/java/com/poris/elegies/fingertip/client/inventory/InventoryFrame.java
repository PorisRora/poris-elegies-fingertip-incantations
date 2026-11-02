package com.poris.elegies.fingertip.client.inventory;

import com.poris.elegies.fingertip.Fingertip;
import net.minecraft.client.Minecraft;

/**
 * One fill per frame, on first use; HUD and editor share the same frame's summary. Nothing is built in a frame that
 * does not ask, as with the HUD hidden or switched off (2026-10-28: it used to be rebuilt every frame regardless).
 */
public final class InventoryFrame {
    private static final InventoryIndex INDEX = new InventoryIndex();
    private static long frame, built = -1;
    public static InventoryIndex index() {
        if (built != frame) { built = frame; INDEX.rebuild(Minecraft.getInstance().player); }
        return INDEX;
    }
    public static void beforeFrame() { frame++; }
    public static void logout() { INDEX.reset(); }
    private InventoryFrame() {}
}

package com.poris.elegies.fingertip.client.hud;

import net.minecraft.client.gui.GuiGraphics;

/** Use the original theme artwork, without a separately layered selection ornament. */
final class NativeHotbarFrame {
    static void draw(GuiGraphics g, int x, int y, boolean selected) {
        UiSkin.slotFrame(true, selected).square(g, x, y, 20);
    }
    private NativeHotbarFrame() {}
}

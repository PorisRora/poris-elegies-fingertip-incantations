package com.poris.elegies.fingertip.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Whole screen pixels for small text and lines, so they stay as crisp as the art at any GUI scale, and the outlined
 * and shortened text drawn with them.
 */
final class GuiPixels {
    /** The dark outline of small numbers on icons: charges, cooldown seconds, durability. */
    static final int OUTLINE = 0xFF15121C;

    /** Screen pixels per GUI pixel, rounded, at least one. */
    static int guiScale() { return Math.max(1, (int)Math.round(Minecraft.getInstance().getWindow().getGuiScale())); }
    /** The scale nearest {@code scale} that puts a whole number of screen pixels on each GUI pixel (at least one). */
    static float scale(float scale) {
        int gui = guiScale();
        return Math.max(1, Math.round(gui * scale)) / (float)gui;
    }
    /** {@code v} GUI px moved to the nearest screen pixel. */
    static float snap(float v) {
        int gui = guiScale();
        return Math.round(v * gui) / (float)gui;
    }
    /** Text without shadow, outlined one font pixel all round in {@code outline}. */
    static void outlined(GuiGraphics g, Font font, String text, int x, int y, int color, int outline) {
        for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++)
            if (dx != 0 || dy != 0) g.drawString(font, text, x + dx, y + dy, outline, false);
        g.drawString(font, text, x, y, color, false);
    }
    private GuiPixels() {}
}

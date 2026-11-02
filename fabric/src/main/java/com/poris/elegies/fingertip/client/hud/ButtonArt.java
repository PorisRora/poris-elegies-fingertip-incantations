package com.poris.elegies.fingertip.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.poris.elegies.fingertip.Fingertip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Matched theme states, sampled from the calibrated button atlases.
 * Poris used flat GUI-pixel geometry until 2026-10-26, when the user asked for its refined art too.
 */
final class ButtonArt {
    private static final int ATLAS_WIDTH = 1536, ATLAS_HEIGHT = 1024, SOURCE_CORNER = 40;
    private static final Atlas CLASSIC = new Atlas("classic", 88, 1130, 1255, 193,
            new int[]{81, 296, 513, 732}, new int[]{184, 185, 185, 185});
    private static final Atlas STARLIGHT = new Atlas("starlight", 78, 1143, 1262, 194,
            new int[]{83, 301, 519, 738}, new int[]{188, 188, 188, 188});
    // Crop rectangles from that batch's manifest.json.
    private static final Atlas PORIS = new Atlas("poris", 97, 1074, 1244, 196,
            new int[]{86, 302, 520, 736}, new int[]{177, 177, 175, 177});

    /** One theme's button atlas: a wide and a compact face, each in four state rows. */
    private record Atlas(ResourceLocation texture, int wideX, int wideWidth, int compactX, int compactWidth,
                         int[] rowY, int[] rowHeight) {
        Atlas(String name, int wideX, int wideWidth, int compactX, int compactWidth, int[] rowY, int[] rowHeight) {
            this(Fingertip.id("textures/gui/themes/buttons/" + name + "-refined-v1.png"),
                    wideX, wideWidth, compactX, compactWidth, rowY, rowHeight);
        }
    }

    static void draw(GuiGraphics g, int x, int y, int width, int height, boolean active, boolean hovered, boolean selected) {
        if (width <= 0 || height <= 0) return;
        var atlas = Theme.current().pick(CLASSIC, STARLIGHT, PORIS);
        int state = !active ? 3 : selected ? 2 : hovered ? 1 : 0;
        boolean compact = width < 64;
        int u = compact ? atlas.compactX : atlas.wideX;
        int sw = compact ? atlas.compactWidth : atlas.wideWidth;
        int v = atlas.rowY[state], sh = atlas.rowHeight[state];
        // Corners are only 3-5 GUI pixels; the quiet center remains available for the label.
        int corner = Math.clamp(Math.round(height * SOURCE_CORNER / (float)sh), 2, 5);
        int bx = Math.min(width / 2, corner), by = Math.min(height / 2, corner);
        int[] dx = {x, x + bx, x + width - bx, x + width};
        int[] dy = {y, y + by, y + height - by, y + height};
        int[] sx = {u, u + SOURCE_CORNER, u + sw - SOURCE_CORNER, u + sw};
        int[] sy = {v, v + SOURCE_CORNER, v + sh - SOURCE_CORNER, v + sh};
        // Flush outstanding text before changing blend state or drawing the next material face.
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try { NineSlice.draw(g, atlas.texture, dx, dy, sx, sy, ATLAS_WIDTH, ATLAS_HEIGHT, true); }
        finally { RenderSystem.disableBlend(); }
    }

    static int textInset(int width, int height) { return width <= 50 ? 3 : width <= 80 ? 4 : 6; }
    private ButtonArt() {}
}

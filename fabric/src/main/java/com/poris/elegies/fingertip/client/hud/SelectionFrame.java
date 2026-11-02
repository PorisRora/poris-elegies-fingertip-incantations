package com.poris.elegies.fingertip.client.hud;

import net.minecraft.client.gui.GuiGraphics;

/** Light editing marks follow the current skin; selected slots keep their own textured frame. */
final class SelectionFrame {
    private record Palette(int fill, int outline, int line, int corner, int guide) {}
    private static final Palette CLASSIC = new Palette(0x14635C4C, 0x60635C4C, 0xC0D0B47A, 0xFFE9CF96, 0xB8D7B77A);
    private static final Palette STARLIGHT = new Palette(0x1654A2C7, 0x605787A8, 0xC099CAE6, 0xFFCDF3FF, 0xB88AD4ED);
    private static final Palette PORIS = new Palette(0x147E253D, 0x607C3D4A, 0xC0C59675, 0xFFF0CC9C, 0xB8D7A17B);
    private static Palette palette() { return Theme.current().pick(CLASSIC, STARLIGHT, PORIS); }
    static void marquee(GuiGraphics g, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        var p = palette();
        g.fill(x, y, x + width, y + height, p.fill);
        EditorTheme.outline(g, x, y, width, height, p.outline);
        dashed(g, x, y, width, height, p.line);
        corners(g, x, y, width, height, 5, p.corner);
    }

    static void slot(GuiGraphics g, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        // Only draw outside the slot. An overlay on its face would tint items and cover counts/keycaps.
        corners(g, x, y, width, height, 3, palette().corner);
    }

    static void group(GuiGraphics g, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        dashed(g, x, y, width, height, palette().line);
    }

    static void guides(GuiGraphics g, Integer guideX, Integer guideY, int top, int width, int height) {
        if (guideX == null && guideY == null || width <= 0 || height <= top) return;
        var p = palette();
        g.enableScissor(0, top, width, height);
        try {
            if (guideX != null) {
                g.fill(guideX - 1, top, guideX + 2, height, p.fill);
                g.fill(guideX, top, guideX + 1, height, p.guide);
                g.fill(guideX - 2, top, guideX + 3, top + 1, p.corner);
                g.fill(guideX - 2, height - 1, guideX + 3, height, p.corner);
            }
            if (guideY != null) {
                g.fill(0, guideY - 1, width, guideY + 2, p.fill);
                g.fill(0, guideY, width, guideY + 1, p.guide);
                g.fill(0, guideY - 2, 1, guideY + 3, p.corner);
                g.fill(width - 1, guideY - 2, width, guideY + 3, p.corner);
            }
        } finally { g.disableScissor(); }
    }

    private static void dashed(GuiGraphics g, int x, int y, int width, int height, int color) {
        for (int offset = 0; offset < width; offset += 8) {
            int end = Math.min(offset + 4, width);
            g.fill(x + offset, y, x + end, y + 1, color);
            if (height > 1) g.fill(x + offset, y + height - 1, x + end, y + height, color);
        }
        for (int offset = 1; offset < height - 1; offset += 8) {
            int end = Math.min(offset + 4, height - 1);
            g.fill(x, y + offset, x + 1, y + end, color);
            if (width > 1) g.fill(x + width - 1, y + offset, x + width, y + end, color);
        }
    }

    private static void corners(GuiGraphics g, int x, int y, int width, int height, int length, int color) {
        int dx = Math.min(length, width), dy = Math.min(length, height);
        g.fill(x, y, x + dx, y + 1, color);
        g.fill(x + width - dx, y, x + width, y + 1, color);
        g.fill(x, y + height - 1, x + dx, y + height, color);
        g.fill(x + width - dx, y + height - 1, x + width, y + height, color);
        g.fill(x, y, x + 1, y + dy, color);
        g.fill(x + width - 1, y, x + width, y + dy, color);
        g.fill(x, y + height - dy, x + 1, y + height, color);
        g.fill(x + width - 1, y + height - dy, x + width, y + height, color);
    }
    private SelectionFrame() {}
}

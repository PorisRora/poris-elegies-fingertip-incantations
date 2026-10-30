package com.poris.elegies.fingertip.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.poris.elegies.fingertip.Fingertip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Continuous nine-slice sampling: corner, edge and surface meet at the same source pixel. */
final class PanelArt {
    private static final int ATLAS_WIDTH = 1774, ATLAS_HEIGHT = 887;
    private static final Face CLASSIC = new Face("classic", 39, 41, 882, 804, 96, 16);
    private static final Face STARLIGHT = new Face("starlight", 38, 38, 852, 810, 120, 20);
    private static final Face PORIS = new Face("poris", 42, 39, 915, 807, 192, 24);

    private record Face(ResourceLocation texture, int u, int v, int width, int height,
                        int sourceCorner, int corner) {
        Face(String theme, int u, int v, int width, int height, int sourceCorner, int corner) {
            this(Fingertip.id("textures/gui/themes/" + theme + "-complete-v1.png"),
                    u, v, width, height, sourceCorner, corner);
        }
    }

    static void draw(GuiGraphics g, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        var face = Theme.current().pick(CLASSIC, STARLIGHT, PORIS);
        // A square source corner always remains square, including short toolbars.
        // Header bars use much smaller ornaments than full dialogs, leaving the same
        // inset available for their title and buttons even at the minimum GUI size.
        int preferred = height <= 70 ? Math.min(face.corner, Math.max(6, height / 5)) : face.corner;
        int corner = Math.min(preferred, Math.min(width, height) / 2);
        int[] dx = {x, x + corner, x + width - corner, x + width};
        int[] dy = {y, y + corner, y + height - corner, y + height};
        int[] sx = {face.u, face.u + face.sourceCorner,
                face.u + face.width - face.sourceCorner, face.u + face.width};
        int[] sy = {face.v, face.v + face.sourceCorner,
                face.v + face.height - face.sourceCorner, face.v + face.height};
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try { NineSlice.draw(g, face.texture, dx, dy, sx, sy, ATLAS_WIDTH, ATLAS_HEIGHT, true); }
        finally { RenderSystem.disableBlend(); }
    }

    private PanelArt() {}
}

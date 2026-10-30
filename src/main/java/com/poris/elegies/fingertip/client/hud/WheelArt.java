package com.poris.elegies.fingertip.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.poris.elegies.fingertip.Fingertip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;

/** One calibrated full-resolution rim per theme; focus samples that same rim, so it cannot jump. */
final class WheelArt {
    // UV windows measured on the untouched original PNGs, including each rim's antialiased lip.
    private static final Rim CLASSIC = new Rim("classic", 14, 13, 1226, 1226, 1254, 1254);
    private static final Rim STARLIGHT = new Rim("starlight", 7, 2, 1240, 1240, 1254, 1254);
    private static final Rim PORIS = new Rim("poris", 9, 6, 1236, 1236, 1254, 1254);

    static void draw(GuiGraphics g, RadialGeometry geometry, int selected) {
        var rim = Theme.current().pick(CLASSIC, STARLIGHT, PORIS);
        g.flush(); RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, rim.texture());
        rim.sector(g, geometry, 0, Math.PI * 2, 0xFFD0D0D0, 160);
        if (selected >= 0) rim.sector(g, geometry, selected * RadialGeometry.STEP - RadialGeometry.STEP / 2,
                RadialGeometry.STEP, 0xFFFFFFFF, 20);
        RenderSystem.disableBlend();
    }

    private record Rim(String theme, int x, int y, int width, int height, int atlasWidth, int atlasHeight) {
        ResourceLocation texture() { return Fingertip.id("textures/gui/themes/wheel-rim-" + theme + "-v2.png"); }
        void sector(GuiGraphics g, RadialGeometry geometry, double start, double span, int color, int steps) {
            var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_TEX_COLOR);
            buffer.addVertex(g.pose().last().pose(), geometry.cx(), geometry.cy(), 0)
                    .setUv((x + width / 2F) / atlasWidth, (y + height / 2F) / atlasHeight).setColor(color);
            // GUI projection flips Y. Traverse the arc backwards so these triangles have the
            // same front-facing winding as GuiGraphics quads and the body triangle strip.
            for (int i = steps; i >= 0; i--) {
                double angle = start + span * i / steps;
                float dx = (float)Math.sin(angle), dy = -(float)Math.cos(angle);
                buffer.addVertex(g.pose().last().pose(), geometry.cx() + dx * geometry.outer(), geometry.cy() + dy * geometry.outer(), 0)
                        .setUv((x + (dx + 1) * width / 2) / atlasWidth, (y + (dy + 1) * height / 2) / atlasHeight).setColor(color);
            }
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        }
    }
    private WheelArt() {}
}

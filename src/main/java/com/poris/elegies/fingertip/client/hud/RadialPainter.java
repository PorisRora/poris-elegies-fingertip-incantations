package com.poris.elegies.fingertip.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import java.util.List;

/** The configuration preview is the real wheel, including sector order and the selected theme. */
final class RadialPainter {
    static void draw(GuiGraphics g, RadialGeometry geometry, List<String> actions, int selected, boolean preview) {
        var palette = EditorTheme.palette();
        g.flush(); RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        // One closed body sits beneath the inner lip of the rim. Sector boundaries are strokes,
        // not holes through to the world, and no oversized polygon outlines protrude past the art.
        float body = geometry.outer() * .90F;
        band(g, geometry, 0, body, 0, Math.PI * 2, palette.surface(), 160);
        if (selected >= 0) band(g, geometry, geometry.inner(), body,
                selected * RadialGeometry.STEP - RadialGeometry.STEP / 2, RadialGeometry.STEP,
                palette.selected(), 20);
        dividers(g, geometry, body, palette.border());
        band(g, geometry, geometry.inner(), geometry.inner() + .7F, 0, Math.PI * 2, palette.border(), 96);
        RenderSystem.disableBlend();
        WheelArt.draw(g, geometry, selected);
        boolean labels = !preview || geometry.outer() >= 90;
        float scale = geometry.outer() < 96 ? .75F : 1F;
        for (int i = 0; i < RadialGeometry.COUNT; i++) {
            int x = geometry.labelX(i), y = geometry.labelY(i);
            boolean available = preview || RadialActions.available(actions.get(i));
            g.pose().pushPose();
            g.pose().translate(x, y, 0); g.pose().scale(scale, scale, 1);
            if (!ActionIcons.draw(g, RadialActions.sprite(actions.get(i)), RadialActions.icon(actions.get(i)), -8, labels ? -13 : -8, !available)
                    && !available) g.fill(-8, labels ? -13 : -8, 8, labels ? 3 : 8, 0x77101820);
            if (labels) fitted(g, RadialActions.label(actions.get(i)).getString(), 7, geometry.labelArc() - 4, scale,
                    !available ? EditorTheme.MUTED : i == selected ? palette.accent() : EditorTheme.TEXT);
            g.pose().popPose();
        }
    }
    /**
     * Text centered on the current origin at {@code y}, no wider than {@code maxWidth} GUI pixels (2026-10-29, user:
     * "轮盘那边英文好像溢出去了" / "我们轮盘可以缩放文字吗": English labels ran into the next sector, and the centre lines past
     * the circle, in Modern UI's wider font). It follows 指尖排字法 (TextFit): shrinks first, only to whole screen pixels per
     * font pixel (at GUI scale 5: 1, 0.8, 0.6), then cuts with an ellipsis; {@code outer} is a scale already applied around
     * it. Text that fits is drawn as before.
     */
    static void fitted(GuiGraphics g, String text, int y, int maxWidth, float outer, int color) {
        var font = Minecraft.getInstance().font;
        var line = TextLines.line(text, (int)(maxWidth / outer), TextFit.fullPixels(outer), font::width);
        g.pose().pushPose();
        try {
            g.pose().scale(line.scale(), line.scale(), 1);
            g.drawCenteredString(font, line.text(), 0, Math.round(y / line.scale()), color);
        } finally { g.pose().popPose(); }
    }
    private static void band(GuiGraphics g, RadialGeometry geometry, float inner, float outer,
                             double start, double span, int color, int steps) {
        var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int n = 0; n <= steps; n++) {
            double angle = start + span * n / steps;
            for (float radius : new float[]{outer, inner}) buffer.addVertex(g.pose().last().pose(),
                    (float)(geometry.cx() + Math.sin(angle) * radius),
                    (float)(geometry.cy() - Math.cos(angle) * radius), 0).setColor(color);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }
    private static void dividers(GuiGraphics g, RadialGeometry geometry, float outer, int color) {
        var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < RadialGeometry.COUNT; i++) {
            double angle = i * RadialGeometry.STEP + RadialGeometry.STEP / 2;
            float dx = (float)Math.sin(angle), dy = -(float)Math.cos(angle), halfWidth = .35F;
            float ix = geometry.cx() + dx * geometry.inner(), iy = geometry.cy() + dy * geometry.inner();
            float ox = geometry.cx() + dx * outer, oy = geometry.cy() + dy * outer;
            float px = -dy * halfWidth, py = dx * halfWidth;
            buffer.addVertex(g.pose().last().pose(), ix + px, iy + py, 0).setColor(color);
            buffer.addVertex(g.pose().last().pose(), ox + px, oy + py, 0).setColor(color);
            buffer.addVertex(g.pose().last().pose(), ox - px, oy - py, 0).setColor(color);
            buffer.addVertex(g.pose().last().pose(), ix - px, iy - py, 0).setColor(color);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }
    private RadialPainter() {}
}

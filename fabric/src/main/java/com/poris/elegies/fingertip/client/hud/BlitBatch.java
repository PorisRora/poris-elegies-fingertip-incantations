package com.poris.elegies.fingertip.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Collects the HUD's plain textured quads while a pass is open and draws each texture and colour in one call
 * (2026-10-28): slot frames and key glyphs used to be a draw call each. The same shader,
 * vertices, texture coordinates and colour as GuiGraphics.blit, with blending on as the callers had it, so the picture
 * does not change; only quads of different textures or colours may swap order, and HUD slots do not overlap.
 */
final class BlitBatch {
    private record Quad(Matrix4f pose, float x1, float x2, float y1, float y2, float u1, float u2, float v1, float v2) {}
    private record Group(ResourceLocation texture, float red, float green, float blue, float alpha) {}
    private static final LinkedHashMap<Group, List<Quad>> QUADS = new LinkedHashMap<>();
    private static boolean open;

    /** Runs the drawer with blits collected, then draws them, texture by texture in first-seen order. */
    static void run(Runnable drawer) {
        if (open) { drawer.run(); return; }
        open = true;
        try { drawer.run(); }
        finally { open = false; draw(); }
    }
    /** The quad blit would draw; false outside a pass, where the caller draws it itself. */
    static boolean add(GuiGraphics g, ResourceLocation texture, int x, int y, int width, int height,
                       float u, float v, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        if (!open) return false;
        float[] color = RenderSystem.getShaderColor();
        QUADS.computeIfAbsent(new Group(texture, color[0], color[1], color[2], color[3]), k -> new ArrayList<>())
                .add(new Quad(new Matrix4f(g.pose().last().pose()), x, x + width, y, y + height,
                        (u + 0.0F) / textureWidth, (u + uWidth) / textureWidth, (v + 0.0F) / textureHeight, (v + vHeight) / textureHeight));
        return true;
    }
    /** A quad with its own normalised texture coordinates, collected as {@link #add} collects a blit; draws at once outside a pass. */
    static void addUv(GuiGraphics g, ResourceLocation texture, float x1, float y1, float x2, float y2, float u1, float v1, float u2, float v2) {
        run(() -> {
            float[] color = RenderSystem.getShaderColor();
            QUADS.computeIfAbsent(new Group(texture, color[0], color[1], color[2], color[3]), k -> new ArrayList<>())
                    .add(new Quad(new Matrix4f(g.pose().last().pose()), x1, x2, y1, y2, u1, u2, v1, v2));
        });
    }
    private static void draw() {
        if (QUADS.isEmpty()) return;
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        try {
            for (var entry : QUADS.entrySet()) {
                var group = entry.getKey();
                RenderSystem.setShaderTexture(0, group.texture());
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderColor(group.red(), group.green(), group.blue(), group.alpha());
                var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                for (var q : entry.getValue()) {
                    buffer.addVertex(q.pose(), q.x1(), q.y1(), 0).setUv(q.u1(), q.v1());
                    buffer.addVertex(q.pose(), q.x1(), q.y2(), 0).setUv(q.u1(), q.v2());
                    buffer.addVertex(q.pose(), q.x2(), q.y2(), 0).setUv(q.u2(), q.v2());
                    buffer.addVertex(q.pose(), q.x2(), q.y1(), 0).setUv(q.u2(), q.v1());
                }
                BufferUploader.drawWithShader(buffer.buildOrThrow());
            }
        } finally {
            QUADS.clear();
            RenderSystem.setShaderColor(1, 1, 1, 1); RenderSystem.disableBlend();
        }
    }
    private BlitBatch() {}
}

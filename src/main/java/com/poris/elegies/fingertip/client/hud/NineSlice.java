package com.poris.elegies.fingertip.client.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * The nine-slice draw of panels, buttons and the entry emblem's frame: the four cuts {@code dx}, {@code dy} on screen
 * against the four {@code sx}, {@code sy} in the image, each part stretched between them. Parts with no area are
 * skipped. Blend state and flushing stay with the caller.
 */
final class NineSlice {
    static void draw(GuiGraphics g, ResourceLocation texture, int[] dx, int[] dy, int[] sx, int[] sy,
                     int atlasWidth, int atlasHeight, boolean centre) {
        for (int row = 0; row < 3; row++) for (int column = 0; column < 3; column++) {
            if (!centre && row == 1 && column == 1) continue;
            int w = dx[column + 1] - dx[column], h = dy[row + 1] - dy[row];
            if (w > 0 && h > 0) g.blit(texture, dx[column], dy[row], w, h, sx[column], sy[row],
                    sx[column + 1] - sx[column], sy[row + 1] - sy[row], atlasWidth, atlasHeight);
        }
    }
    private NineSlice() {}
}

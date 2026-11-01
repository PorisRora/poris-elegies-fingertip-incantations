package com.poris.elegies.fingertip.client.hud;

import com.poris.elegies.fingertip.client.BindingFeedback;
import com.poris.elegies.fingertip.client.ClientSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Visual feedback shared by the layout surface and the binding panel. */
final class BindingPresentation {
    static void target(GuiGraphics g, int id, int x, int y, ItemStack icon, ActionIcons.Icon sprite) {
        if (com.poris.elegies.fingertip.client.input.HotbarControl.nativeSlot(id)) {
            g.pose().pushPose(); g.pose().translate(0, 0, 500);
            EditorTheme.outline(g, x - 1, y - 1, 22, 22, ThemePalette.ERROR);
            g.drawString(Minecraft.getInstance().font, "×", x + 15, y - 7, ThemePalette.ERROR, true);
            g.pose().popPose(); return;
        }
        g.pose().pushPose(); g.pose().translate(0, 0, 300);
        HudRenderer.frame(g, x, y, true);
        if (sprite != null) UiSkin.sprite(g, sprite, x + 2, y + 2, 20, false);
        else UiSkin.item(g, icon, x + 2, y + 2, 20, false);
        g.pose().translate(0, 0, 200);
        SelectionFrame.slot(g, x - 1, y - 1, 22, 22);
        boolean replace = ClientSession.binding(id).kind() != com.poris.elegies.fingertip.binding.HudBinding.Kind.EMPTY;
        g.drawString(Minecraft.getInstance().font, replace ? "↔" : "+", x + 15, y - 7, EditorTheme.accent(), true);
        g.pose().popPose();
    }
    /** The latest notice as a banner at {@code y}, or the nearest line from there, {@code step} at a time, that covers no frame. */
    static void feedback(GuiGraphics g, int y, int step, java.util.List<HudPlacement.Rect> avoid) {
        var notice = BindingFeedback.latest();
        if (notice != null) banner(g, notice.text(), clearY(g, notice.text(), y, step, avoid), color(notice));
    }
    static int color(BindingFeedback.Notice notice) {
        return switch (notice.state()) { case WAITING -> ThemePalette.PENDING; case SUCCESS -> ThemePalette.READY; case ERROR -> ThemePalette.ERROR; };
    }
    static void banner(GuiGraphics g, Component message, int y, int step, java.util.List<HudPlacement.Rect> avoid) {
        banner(g, message, clearY(g, message, y, step, avoid), EditorTheme.accent());
    }
    /** {@code y}, or the nearest line from there, {@code step} at a time, where the banner covers no frame (指尖定位法). */
    private static int clearY(GuiGraphics g, Component message, int y, int step, java.util.List<HudPlacement.Rect> avoid) {
        var font = Minecraft.getInstance().font;
        int w = Math.min(g.guiWidth() - 12, font.width(message) + 32), x = (g.guiWidth() - w) / 2;
        var candidates = new java.util.ArrayList<HudPlacement.Rect>();
        for (int top = y; top >= 0 && top + 22 <= g.guiHeight(); top += step) candidates.add(new HudPlacement.Rect(x, top, w, 22));
        var spot = HudPlacement.bestSpot(candidates, avoid, 2, g.guiWidth(), g.guiHeight());
        return spot == null ? y : spot.y();
    }
    private static void banner(GuiGraphics g, Component message, int y, int color) {
        var font = Minecraft.getInstance().font;
        int w = Math.min(g.guiWidth() - 12, font.width(message) + 32), x = (g.guiWidth() - w) / 2;
        g.pose().pushPose(); g.pose().translate(0, 0, 600);
        EditorTheme.panel(g, x, y, w, 22, true);
        TextFit.draw(g, font, message, x + 16, y + 7, Math.max(0, w - 32), color, false, TextFit.Align.CENTER);
        g.pose().popPose();
    }
    static void slotFeedback(GuiGraphics g, int id) {
        var notice = BindingFeedback.latest();
        if (notice == null || notice.hud() != id) return;
        g.pose().pushPose(); g.pose().translate(0, 0, 400);
        EditorTheme.outline(g, -1, -1, 22, 22, color(notice));
        String marker = switch (notice.state()) { case WAITING -> "…"; case SUCCESS -> "✓"; case ERROR -> "!"; };
        var font = Minecraft.getInstance().font;
        g.fill(12, -5, 22, 5, 0xF01A1823);
        g.drawString(font, marker, 17 - (font.width(marker) - 1) / 2, -4, color(notice), false);   // centered in the tag
        g.pose().popPose();
    }
    private BindingPresentation() {}
}

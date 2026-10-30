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
            EditorTheme.outline(g, x - 1, y - 1, 22, 22, 0xFFE5A69B);
            g.drawString(Minecraft.getInstance().font, "×", x + 15, y - 7, 0xFFE5A69B, true);
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
    static void feedback(GuiGraphics g, int y) {
        var notice = BindingFeedback.latest();
        if (notice != null) banner(g, notice.text(), y, color(notice));
    }
    static int color(BindingFeedback.Notice notice) {
        return switch (notice.state()) { case WAITING -> EditorTheme.accent(); case SUCCESS -> 0xFFB7DEAE; case ERROR -> 0xFFE5A69B; };
    }
    static void banner(GuiGraphics g, Component message, int y) { banner(g, message, y, EditorTheme.accent()); }
    private static void banner(GuiGraphics g, Component message, int y, int color) {
        var font = Minecraft.getInstance().font;
        int w = Math.min(g.guiWidth() - 12, font.width(message) + 32), x = (g.guiWidth() - w) / 2;
        g.pose().pushPose(); g.pose().translate(0, 0, 600);
        EditorTheme.panel(g, x, y, w, 22, true);
        var text = font.plainSubstrByWidth(message.getString(), Math.max(0, w - 32));
        g.drawString(font, text, x + (w - font.width(text)) / 2, y + 7, color, false);
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

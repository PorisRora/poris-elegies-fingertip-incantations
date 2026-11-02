package com.poris.elegies.fingertip.client.hud;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;

/** Destructive edits have the same theme, spacing and explicit choice as the editor. */
final class ThemedConfirmScreen extends ConfirmScreen {
    private final BooleanConsumer choice;
    private final Component heading, detail;
    private int x, y, w, h;
    private java.util.List<String> headingLines = java.util.List.of(), detailLines = java.util.List.of();
    private boolean decided;

    ThemedConfirmScreen(BooleanConsumer choice, Component title, Component detail) {
        super(choice, title, detail); this.choice = choice; this.heading = title; this.detail = detail;
    }
    @Override protected void init() {
        w = Math.min(380, width - 32);
        // The box grows with its text up to the screen; text that still does not fit ends with "…" above the buttons,
        // instead of running out of the box under them (指尖排字法).
        int room = w - 32, lines = Math.max(2, (height - 24 - 68) / 12);
        headingLines = TextLines.wrap(heading.getString(), room, Math.min(3, lines - 1), font::width).lines();
        detailLines = TextLines.wrap(detail.getString(), room, lines - headingLines.size(), font::width).lines();
        h = Math.min(height - 24, 68 + (headingLines.size() + detailLines.size()) * 12);
        x = (width - w) / 2; y = (height - h) / 2;
        int bw = (w - 40) / 2;
        addRenderableWidget(EditorTheme.button(x + 16, y + h - 34, bw, 20, Component.translatable("gui.yes"), b -> decide(true)));
        addRenderableWidget(EditorTheme.button(x + w - 16 - bw, y + h - 34, bw, 20, Component.translatable("gui.no"), b -> decide(false)));
    }
    private void decide(boolean value) { if (!decided) { decided = true; choice.accept(value); } }
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, 0xB0100F19); EditorTheme.panel(g, x, y, w, h, true);
    }
    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g, mx, my, partial);
        int textY = y + 14;
        for (var line : headingLines) { g.drawString(font, line, x + 16, textY, EditorTheme.accent(), false); textY += 12; }
        textY += 10;
        for (var line : detailLines) { g.drawString(font, line, x + 16, textY, EditorTheme.TEXT, false); textY += 12; }
        net.fabricmc.fabric.api.client.screen.v1.Screens.getButtons(this).forEach(widget -> widget.render(g, mx, my, partial));
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { decide(false); }
}

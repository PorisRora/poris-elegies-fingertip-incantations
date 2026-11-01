package com.poris.elegies.fingertip.client.hud;

import com.poris.elegies.fingertip.Fingertip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 指尖排字法 (1.1.0; 2026-11-01): what text does when it does not fit, the same
 * everywhere. Before, some lines were cut silently, some cut with "…", the wheel shrank its labels, the key box dropped
 * lines, the confirm box let them run out of its frame and buttons scrolled.
 * <ul>
 * <li>One line shrinks first, only to scales that put whole screen pixels on each font pixel and never below 60 %,
 * then is cut with "…"; pointing at a cut line shows its full text (buttons add it to their hover text).</li>
 * <li>Several lines wrap, up to the lines the place has; the last ends with "…" when text is left over.</li>
 * <li>Lines break after "·" and punctuation first, then between Chinese characters or at a space, never inside a
 * word or a number, nor between a number and what touches it: Modern UI breaks between any two Chinese characters,
 * which could split "第 12 格".</li>
 * </ul>
 * Hover text keeps vanilla's own wrapping.
 */
@EventBusSubscriber(modid = Fingertip.MOD_ID, value = Dist.CLIENT)
public final class TextFit {
    /** Where a line sits in its room. */
    enum Align { LEFT, CENTER }
    // ---- drawing ----
    /** Screen pixels on each font pixel here, with {@code outer} the scale already applied around the text. */
    static int fullPixels(float outer) { return Math.max(1, Math.round(outer * GuiPixels.guiScale())); }
    static TextLines.Line fit(Font font, String text, int room) { return TextLines.line(text, Math.max(0, room), fullPixels(1), font::width); }
    /**
     * One line in the room from {@code x} to {@code x + room}, at {@code y}. Returns whether it was cut; a cut line is
     * remembered, and pointing at it shows the whole text.
     */
    static boolean draw(GuiGraphics g, Font font, String text, int x, int y, int room, int color, boolean shadow, Align align) {
        var line = fit(font, text, room);
        int w = Math.round(font.width(line.text()) * line.scale());
        int left = switch (align) { case LEFT -> x; case CENTER -> x + (room - w) / 2; };
        drawLine(g, font, line, left, y, color, shadow);
        if (line.cut()) remember(left, y, Math.max(1, w), font.lineHeight, text);
        return line.cut();
    }
    static boolean draw(GuiGraphics g, Font font, Component text, int x, int y, int room, int color, boolean shadow, Align align) {
        return draw(g, font, text.getString(), x, y, room, color, shadow, align);
    }
    /** A fitted line at {@code left}, {@code y}; a shrunk one stays centred on the line it would have filled. */
    static void drawLine(GuiGraphics g, Font font, TextLines.Line line, float left, float y, int color, boolean shadow) {
        if (line.scale() == 1) { g.drawString(font, line.text(), Math.round(left), Math.round(y), color, shadow); return; }
        g.pose().pushPose();
        try {
            g.pose().translate(GuiPixels.snap(left), GuiPixels.snap(y + font.lineHeight * (1 - line.scale()) / 2), 0);
            g.pose().scale(line.scale(), line.scale(), 1);
            g.drawString(font, line.text(), 0, 0, color, shadow);
        } finally { g.pose().popPose(); }
    }
    /** Up to {@code maxLines} lines from ({@code x}, {@code y}), {@code lineHeight} apart. Returns the lines drawn. */
    static int drawLines(GuiGraphics g, Font font, String text, int x, int y, int room, int maxLines, int lineHeight, int color, boolean shadow) {
        var wrapped = TextLines.wrap(text, room, maxLines, font::width);
        for (int i = 0; i < wrapped.lines().size(); i++) g.drawString(font, wrapped.lines().get(i), x, y + i * lineHeight, color, shadow);
        if (wrapped.cut()) remember(x, y, room, wrapped.lines().size() * lineHeight, text);
        return wrapped.lines().size();
    }
    static int drawLines(GuiGraphics g, Font font, Component text, int x, int y, int room, int maxLines, int lineHeight, int color, boolean shadow) {
        return drawLines(g, font, text.getString(), x, y, room, maxLines, lineHeight, color, shadow);
    }
    /** How many lines of {@code lineHeight} fit in {@code height} (text {@code font.lineHeight} tall in the last). */
    static int linesIn(Font font, int height, int lineHeight) { return height < font.lineHeight ? 0 : 1 + (height - font.lineHeight) / lineHeight; }

    // ---- the full text of a cut line, shown when pointed at ----
    private record Cut(int x, int y, int width, int height, String text) {}
    private static final List<Cut> CUTS = new ArrayList<>();
    private static void remember(int x, int y, int width, int height, String text) {
        if (CUTS.size() < 64) CUTS.add(new Cut(x, y, width, height, text));
    }
    @SubscribeEvent public static void beforeScreen(ScreenEvent.Render.Pre event) { CUTS.clear(); }
    @SubscribeEvent public static void afterScreen(ScreenEvent.Render.Post event) {
        int mx = event.getMouseX(), my = event.getMouseY();
        for (int i = CUTS.size() - 1; i >= 0; i--) {
            var cut = CUTS.get(i);
            if (mx < cut.x() || mx >= cut.x() + cut.width() || my < cut.y() || my >= cut.y() + cut.height()) continue;
            var g = event.getGuiGraphics();
            var font = Minecraft.getInstance().font;
            g.pose().pushPose(); g.pose().translate(0, 0, 600);
            try { g.renderTooltip(font, font.split(Component.literal(cut.text()), Math.max(40, Math.min(240, g.guiWidth() - 24))), mx, my); }
            finally { g.pose().popPose(); }
            return;
        }
    }
    private TextFit() {}
}

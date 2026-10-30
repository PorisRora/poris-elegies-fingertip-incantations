package com.poris.elegies.fingertip.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import java.util.function.BooleanSupplier;

/** The editor's themed controls and colours; text, hit areas and input behavior stay at the user's GUI scale. */
final class EditorTheme {
    static final int TEXT = 0xFFF1E9D8, MUTED = 0xFFA9A2B7;
    static ThemePalette palette() { return ThemePalette.of(Theme.current()); }
    static int accent() { return palette().accent(); }
    static void panel(GuiGraphics g, int x, int y, int w, int h, boolean raised) {
        if (raised) {
            PanelArt.draw(g, x, y, w, h);
        } else {
            // Secondary content stays quiet so repeated frames do not compete with the item grid.
            g.fill(x, y, x + w, y + h, palette().border());
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, palette().surface());
        }
    }
    static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color); g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color); g.fill(x + w - 1, y, x + w, y + h, color);
    }
    static void slot(GuiGraphics g, int x, int y, boolean hovered) {
        (hovered ? UiSkin.SLOT_HOVER : UiSkin.SLOT).square(g, x, y, 18);
    }
    static boolean hoverOrKeyboardFocus(AbstractWidget widget) {
        return widget.isHovered() || widget.isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard();
    }
    static Button button(int x, int y, int w, int h, Component text, Button.OnPress action) {
        return button(x, y, w, h, text, action, () -> false);
    }
    /** Shared face for ordinary controls and rows that position their own icon/label. */
    static int buttonFace(GuiGraphics g, int x, int y, int w, int h, boolean active, boolean hovered, boolean selected) {
        ButtonArt.draw(g, x, y, w, h, active, hovered, selected);
        return textInset(w, h);
    }
    static int textInset(int w, int h) { return ButtonArt.textInset(w, h); }
    static Button button(int x, int y, int w, int h, Component text, Button.OnPress action, BooleanSupplier selected) {
        return new Button(x, y, w, h, text, action, java.util.function.Supplier::get) {
            @Override protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
                boolean chosen = selected.getAsBoolean();
                boolean hovered = hoverOrKeyboardFocus(this);
                boolean lit = active && (hovered || chosen);
                int margin = buttonFace(g, getX(), getY(), getWidth(), getHeight(), active, hovered, chosen);
                renderScrollingString(g, Minecraft.getInstance().font, margin, active ? (lit ? accent() : TEXT) : 0xFF918A9B);
            }
        };
    }
    /** The icon sits left of the label and the pair is centred; without room for both only the label is drawn. */
    static Button button(int x, int y, int w, int h, Component text, ActionIcons.Sprite icon, Button.OnPress action) {
        return new Button(x, y, w, h, text, action, java.util.function.Supplier::get) {
            @Override protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
                boolean hovered = hoverOrKeyboardFocus(this);
                int margin = buttonFace(g, getX(), getY(), getWidth(), getHeight(), active, hovered, false);
                int color = active ? (hovered ? accent() : TEXT) : 0xFF918A9B;
                var font = Minecraft.getInstance().font;
                int group = ActionIcons.SMALL + ICON_GAP + font.width(getMessage());
                if (getHeight() < ActionIcons.SMALL + 4 || group > getWidth() - margin * 2) {
                    renderScrollingString(g, font, margin, color); return;
                }
                int left = getX() + (getWidth() - group) / 2;
                icon.drawSmall(g, left, getY() + (getHeight() - ActionIcons.SMALL) / 2);
                g.drawString(font, getMessage(), left + ActionIcons.SMALL + ICON_GAP, getY() + (getHeight() - 9) / 2 + 1, color);
            }
        };
    }
    static final int ICON_GAP = 4;
    private EditorTheme() {}
}

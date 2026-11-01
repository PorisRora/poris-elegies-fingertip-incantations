package com.poris.elegies.fingertip.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
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
    static ThemedButton button(int x, int y, int w, int h, Component text, Button.OnPress action) {
        return button(x, y, w, h, text, action, () -> false);
    }
    /** Shared face for ordinary controls and rows that position their own icon/label. */
    static int buttonFace(GuiGraphics g, int x, int y, int w, int h, boolean active, boolean hovered, boolean selected) {
        ButtonArt.draw(g, x, y, w, h, active, hovered, selected);
        return textInset(w, h);
    }
    static int textInset(int w, int h) { return ButtonArt.textInset(w, h); }
    static ThemedButton button(int x, int y, int w, int h, Component text, Button.OnPress action, BooleanSupplier selected) {
        return new ThemedButton(x, y, w, h, text, action, selected, null);
    }
    /** The icon sits left of the label and the pair is centred; without room for both only the label is drawn. */
    static ThemedButton button(int x, int y, int w, int h, Component text, ActionIcons.Sprite icon, Button.OnPress action) {
        return new ThemedButton(x, y, w, h, text, action, () -> false, icon);
    }
    /** Hover text for a control; a themed button whose label gets cut shows the full label above it (指尖排字法). */
    static Tooltip tip(Component text) {
        var tooltip = Tooltip.create(text);
        TIPS.put(tooltip, text);
        return tooltip;
    }
    private static final java.util.Map<Tooltip, Component> TIPS = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    /**
     * The editor's button. Its label follows 指尖排字法 instead of vanilla's scrolling (2026-11-01, user agreed): it
     * shrinks, then is cut with "…", and a cut label is shown whole in the hover text, before any hint.
     */
    static final class ThemedButton extends Button {
        private final BooleanSupplier selected;
        private final ActionIcons.Sprite icon;
        private Component hint, shownFor;
        private Tooltip external;
        private boolean cut, settled;
        ThemedButton(int x, int y, int w, int h, Component text, OnPress action, BooleanSupplier selected, ActionIcons.Sprite icon) {
            super(x, y, w, h, text, action, java.util.function.Supplier::get);
            this.selected = selected; this.icon = icon;
        }
        @Override public void setTooltip(Tooltip tooltip) {
            hint = tooltip == null ? null : TIPS.get(tooltip);
            external = hint == null ? tooltip : null;
            settled = false;
            super.setTooltip(tooltip);
        }
        @Override protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
            boolean chosen = selected.getAsBoolean();
            boolean hovered = hoverOrKeyboardFocus(this);
            int margin = buttonFace(g, getX(), getY(), getWidth(), getHeight(), active, hovered, chosen);
            int color = active ? (hovered || chosen ? accent() : TEXT) : ThemePalette.PAUSED;
            var font = Minecraft.getInstance().font;
            int textY = getY() + (getHeight() - 9) / 2 + 1;
            if (icon != null && showsWhole(font, getMessage(), getWidth(), getHeight())) {
                int left = getX() + (getWidth() - ActionIcons.SMALL - ICON_GAP - font.width(getMessage())) / 2;
                icon.drawSmall(g, left, getY() + (getHeight() - ActionIcons.SMALL) / 2);
                g.drawString(font, getMessage(), left + ActionIcons.SMALL + ICON_GAP, textY, color);
                settle(false); return;
            }
            int room = getWidth() - margin * 2;
            var line = TextFit.fit(font, getMessage().getString(), room);
            float w = font.width(line.text()) * line.scale();
            TextFit.drawLine(g, font, line, getX() + margin + (room - w) / 2, textY, color, true);
            settle(line.cut());
        }
        /** Keeps the hover text in step with whether the label is cut. */
        private void settle(boolean nowCut) {
            if (settled && nowCut == cut && getMessage().equals(shownFor)) return;
            settled = true; cut = nowCut; shownFor = getMessage();
            if (external != null) { super.setTooltip(external); return; }   // a tooltip set from elsewhere stays as it was
            Component text = !cut ? hint : hint == null ? getMessage() : getMessage().copy().append("\n").append(hint);
            super.setTooltip(text == null ? null : Tooltip.create(text));
        }
    }
    static final int ICON_GAP = 4;
    /** Whether a button this size shows its icon and its whole label at full size, as ThemedButton draws it first. */
    static boolean showsWhole(net.minecraft.client.gui.Font font, Component text, int w, int h) {
        return h >= ActionIcons.SMALL + 4 && ActionIcons.SMALL + ICON_GAP + font.width(text) <= w - textInset(w, h) * 2;
    }
    private EditorTheme() {}
}

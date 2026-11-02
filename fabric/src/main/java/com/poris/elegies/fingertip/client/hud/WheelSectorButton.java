package com.poris.elegies.fingertip.client.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Narratable keyboard-focusable sector whose hit area is its wedge, not an overlapping rectangle. It has no widget
 * tooltip: vanilla keeps those off the widget's box, which is the whole wheel, so they sat above it; the screen draws the
 * hovered sector's text at the mouse instead (0.2.3-beta).
 */
final class WheelSectorButton extends Button {
    private final RadialGeometry geometry;
    private final int sector;
    WheelSectorButton(RadialGeometry geometry, int sector, Component label, OnPress press) {
        super(geometry.cx() - geometry.outer(), geometry.cy() - geometry.outer(), geometry.outer() * 2, geometry.outer() * 2,
                label, press, java.util.function.Supplier::get);
        this.geometry = geometry; this.sector = sector;
    }
    @Override public boolean isMouseOver(double x, double y) { return visible && geometry.atPreview(x, y) == sector; }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (!active || !isMouseOver(x, y) || button != 0) return false;
        playDownSound(net.minecraft.client.Minecraft.getInstance().getSoundManager()); onPress(); return true;
    }
    @Override protected void renderWidget(GuiGraphics g, int x, int y, float partial) { isHovered = isMouseOver(x, y); }
    int sector() { return sector; }
}

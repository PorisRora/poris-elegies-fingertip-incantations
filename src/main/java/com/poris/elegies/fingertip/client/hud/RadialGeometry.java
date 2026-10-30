package com.poris.elegies.fingertip.client.hud;

import com.poris.elegies.fingertip.client.input.InputPreferences;

/** Runtime and settings share one clockwise, top-first ten-sector coordinate system. */
public record RadialGeometry(int cx, int cy, int inner, int outer) {
    public static final int COUNT = InputPreferences.WHEEL_SLOTS;
    public static final double STEP = Math.PI * 2 / COUNT;
    public static RadialGeometry fit(int width, int height) {
        int radius = Math.max(45, Math.min(124, Math.min(width / 2 - 8, height / 2 - 18)));
        return new RadialGeometry(width / 2, height / 2, Math.max(20, radius * 34 / 100), radius);
    }
    public int at(double x, double y) {
        return at(x, y, 20);
    }
    public int atPreview(double x, double y) { return at(x, y, 0); }
    private int at(double x, double y, int margin) {
        double dx = x - cx, dy = y - cy;
        double distance = Math.hypot(dx, dy);
        if (distance < inner || distance > outer + margin) return -1;
        double angle = Math.atan2(dx, -dy);
        return Math.floorMod((int)Math.floor((angle + STEP / 2) / STEP), COUNT);
    }
    private double labelRadius() { return (inner + outer) / 2.0 - Math.max(2, outer / 18); }
    /** One sector's width along the label circle, what a label may span before it reaches the next sector. */
    public int labelArc() { return (int)Math.floor(2 * Math.PI * labelRadius() / COUNT); }
    /** The width inside the centre circle for a text line from {@code top} to {@code bottom}, relative to the centre. */
    public int centreWidth(int top, int bottom) {
        int dy = Math.max(Math.abs(top), Math.abs(bottom));
        return dy >= inner ? 0 : (int)Math.floor(2 * Math.sqrt((double)inner * inner - (double)dy * dy));
    }
    public int labelX(int sector) { return cx + (int)Math.round(Math.sin(sector * STEP) * labelRadius()); }
    public int labelY(int sector) { return cy - (int)Math.round(Math.cos(sector * STEP) * labelRadius()); }
}

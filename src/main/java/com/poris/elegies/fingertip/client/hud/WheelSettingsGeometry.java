package com.poris.elegies.fingertip.client.hud;

/** Keeps the spatial preview and action picker separate even at Minecraft's smallest GUI size. */
record WheelSettingsGeometry(RadialGeometry wheel, int pickerX, int pickerY, int pickerWidth, int rowStep, int buttonHeight, int hintY) {
    static WheelSettingsGeometry fit(int x, int top, int w, int bottom) {
        int availableHeight = bottom - top;
        int leftWidth = (w - 28) * 45 / 100;
        int radius = Math.max(32, Math.min(112, Math.min(leftWidth / 2 - 4, availableHeight / 2 - 3)));
        var wheel = new RadialGeometry(x + 10 + leftWidth / 2, top + availableHeight / 2,
                Math.max(17, radius * 34 / 100), radius);
        int px = x + 18 + leftWidth, pw = x + w - 10 - px;
        int step = availableHeight < 150 ? 19 : 26, bh = step - (step < 20 ? 2 : 5);
        return new WheelSettingsGeometry(wheel, px, top + 15, pw, step, bh, top + 15 + 5 * step + 5);
    }
}

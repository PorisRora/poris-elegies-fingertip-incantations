package com.poris.elegies.fingertip.client.hud;

/** The wheel owns its own page; action choices are a separate screen. */
final class WheelConfigurationGeometry {
    static final int MARGIN = 16, DONE_WIDTH = 64, MODE_WIDTH = 80, GAP = 8, BUTTON_HEIGHT = 20;
    final RadialGeometry wheel;
    /**
     * One footer row: the drag/click switch at the left end (2026-10-29), the wheel switch centred
     * under the wheel, narrower rather than under the first, Done against the right edge.
     */
    final int footerY, modeX, toggleX, toggleWidth, doneX;

    WheelConfigurationGeometry(int width, int height) {
        int top = 48;   // room for the hint under the title in every window (1.1.5; below 270 high it was left out)
        int bottom = height - 40;
        int radius = Math.min(132, Math.min((width - 32) / 2, (bottom - top) / 2));
        wheel = new RadialGeometry(width / 2, (top + bottom) / 2, Math.max(18, radius * 34 / 100), radius);
        footerY = height - 28;
        doneX = width - MARGIN - DONE_WIDTH;
        modeX = MARGIN;
        int half = Math.min(64, Math.min(doneX - GAP - wheel.cx(), wheel.cx() - modeX - MODE_WIDTH - GAP));
        toggleX = wheel.cx() - half; toggleWidth = half * 2;
    }
}

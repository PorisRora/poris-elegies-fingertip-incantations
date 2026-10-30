package com.poris.elegies.fingertip.client.hud;

/** Shared bounds for the layout toolbar, its groups, and the unobstructed canvas. */
final class LayoutToolbarGeometry {
    private static final int[] GROUP_WIDTHS = {92, 128, 132, 116};
    static final int DONE_WIDTH = 64;
    final int x, y, width, height;
    final boolean compact;

    LayoutToolbarGeometry(int screenWidth) {
        width = Math.min(528, screenWidth - 16);
        x = (screenWidth - width) / 2;
        compact = width < 528;
        y = compact ? 8 : 14;
        height = compact ? 112 : 76;
    }
    int groupX(int group) {
        if (compact) return x + 12 + (group % 2) * ((width - 36) / 2 + 12);
        int offset = 12;
        for (int i = 0; i < group; i++) offset += GROUP_WIDTHS[i] + 12;
        return x + offset;
    }
    int groupWidth(int group) { return GROUP_WIDTHS[group]; }
    int labelY(int group) { return y + 36 + (compact ? group / 2 * 36 : 0); }
    int buttonY(int group) { return labelY(group) + 12; }
    int canvasTop() { return y + height + 6; }
    /** Done sits in the top right corner, 12 in from the toolbar's edge. */
    int doneX() { return x + width - 12 - DONE_WIDTH; }
    boolean contains(double px, double py) { return EditorGeometry.inside(px, py, x, y, width, height); }
}

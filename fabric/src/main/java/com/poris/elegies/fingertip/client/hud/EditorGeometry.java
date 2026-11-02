package com.poris.elegies.fingertip.client.hud;

import java.util.function.Predicate;

/**
 * The library and inspector float above the real HUD; no miniature coordinate transform is used. Two sizes, as
 * Minecraft's recipe book has two layouts: the normal one wherever it fits, else the compact one, made for Minecraft's
 * smallest GUI (320 x 240); Minecraft's GUI scale makes either larger or smaller as it does vanilla screens. (1.1.6,
 * 2026-11-01. 1.1.5 had kept only the compact one, taking the user's screen
 * for 512 x 274, vanilla's auto GUI scale on 2560 x 1369; Modern UI's gives 640 x 343 there, and 640 x 372 on the laptop,
 * where the normal one fits.)
 */
final class EditorGeometry {
    /** Two lines of the items page's hint: the font's 9 pixels and 11 to the next line. */
    static final int HINT_ROOM = 20;
    final int x, y, width, height, rightX, rightWidth, tabsY, listY, bodyTop, bodyBottom;
    final int inventoryCell, inventoryPitch, inventoryWidth;
    final int headerHeight, headerInset, toolbarGap, toolbarButtonHeight, toolbarY, panelPad;
    final boolean compact;

    /**
     * The normal size when it fits, else compact (2026-11-01). The normal layout is
     * tried on the window first and kept when the items page has two lines of hint above the HUD row and
     * {@code textsFit} says the title and the tool buttons show their text whole in it; the editor answers that with its
     * font.
     */
    static EditorGeometry of(int screenWidth, int screenHeight, Predicate<EditorGeometry> textsFit) {
        var normal = new EditorGeometry(screenWidth, screenHeight, false);
        return normal.hintHeight() >= HINT_ROOM && textsFit.test(normal) ? normal : new EditorGeometry(screenWidth, screenHeight, true);
    }

    private EditorGeometry(int screenWidth, int screenHeight, boolean compact) {
        this.compact = compact;
        int outer = compact ? 8 : 14, panelGap = compact ? 6 : 10;
        x = outer; y = outer; width = screenWidth - outer * 2; height = screenHeight - outer * 2;
        panelPad = compact ? 5 : 10; headerInset = compact ? 6 : 10;
        headerHeight = compact ? 50 : 60; toolbarGap = compact ? 4 : 6;
        toolbarButtonHeight = compact ? 16 : 18; toolbarY = y + (compact ? 28 : 34);
        inventoryCell = compact ? 16 : 22; inventoryPitch = compact ? 17 : 24;
        inventoryWidth = 8 * inventoryPitch + inventoryCell;
        rightWidth = inventoryWidth; rightX = screenWidth - outer - panelPad - rightWidth;
        bodyTop = y + headerHeight + panelGap + panelPad; tabsY = bodyTop; listY = tabsY + 22;
        // The compact source library needs a complete group header and card; dragging or Tab already exposes the HUD
        // underneath, so do not reserve an unusable blank footer here. 137 rather than 126 leaves the items page two lines
        // for its hint (指尖排字法, 2026-11-01).
        bodyBottom = compact ? Math.min(screenHeight - 8 - panelPad, bodyTop + 137)
                : Math.min(screenHeight - 38, bodyTop + 220);
    }
    int toolbarWidth() { return (width - headerInset * 2 - toolbarGap * 3) / 4; }
    int toolbarX(int index) { return x + headerInset + index * (toolbarWidth() + toolbarGap); }
    int enableWidth() { return compact ? 50 : 78; }
    /**
     * 86 fits the full label ("模式：指尖"). A window that is short but wide (2560x1369 at vanilla's GUI scale 5 is
     * 512x274) is compact only by height, so it still gets 86 while the title keeps 120 (2026-10-27).
     */
    int modeWidth() {
        int titleRoom = width - headerInset * 2 - doneWidth() - enableWidth() - toolbarGap * 4 - 86;
        return !compact || titleRoom >= 120 ? 86 : 60;
    }
    int doneWidth() { return compact ? 34 : 42; }
    int doneX() { return x + width - headerInset - doneWidth(); }
    int modeX() { return doneX() - toolbarGap - modeWidth(); }
    int enableX() { return modeX() - toolbarGap - enableWidth(); }
    int titleWidth() { return enableX() - x - headerInset - toolbarGap * 2; }
    int inventoryX() { return rightX + (rightWidth - inventoryWidth) / 2; }
    /** The items page's hint: where it starts, below the hotbar row, and how tall it may be. */
    int hintY() { return inventoryY(0) + inventoryCell + 10; }
    int hintHeight() { return bodyBottom - hintY() - 6; }
    int inventoryY(int index) { return listY + (index < 9 ? 3 * inventoryPitch + 4 : ((index - 9) / 9) * inventoryPitch); }
    int inventoryX(int index) { return inventoryX() + index % 9 * inventoryPitch; }
    int inventoryAt(double px, double py) {
        for (int index = 0; index < 36; index++)
            if (inside(px, py, inventoryX(index), inventoryY(index), inventoryCell, inventoryCell)) return index;
        return -1;
    }
    static boolean inside(double px, double py, int x, int y, int w, int h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }
}

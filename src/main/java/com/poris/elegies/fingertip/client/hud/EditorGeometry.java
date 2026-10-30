package com.poris.elegies.fingertip.client.hud;

/** The library and inspector float above the real HUD; no miniature coordinate transform is used. */
final class EditorGeometry {
    final int x, y, width, height, rightX, rightWidth, tabsY, listY, bodyTop, bodyBottom;
    final int inventoryCell, inventoryPitch, inventoryWidth;
    final int headerHeight, headerInset, toolbarGap, toolbarButtonHeight, toolbarY, panelPad;
    final boolean compact;

    EditorGeometry(int screenWidth, int screenHeight, int slotCount) {
        compact = screenHeight < 320 || screenWidth < 470;
        int outer = compact ? 8 : 14, panelGap = compact ? 6 : 10;
        x = outer; y = outer; width = screenWidth - outer * 2; height = screenHeight - outer * 2;
        panelPad = compact ? 5 : 10; headerInset = compact ? 6 : 10;
        headerHeight = compact ? 50 : 60; toolbarGap = compact ? 4 : 6;
        toolbarButtonHeight = compact ? 16 : 18; toolbarY = y + (compact ? 28 : 34);
        inventoryCell = compact ? 16 : 22; inventoryPitch = compact ? 17 : 24;
        inventoryWidth = 8 * inventoryPitch + inventoryCell;
        rightWidth = inventoryWidth; rightX = screenWidth - outer - panelPad - rightWidth;
        bodyTop = y + headerHeight + panelGap + panelPad; tabsY = bodyTop; listY = tabsY + 22;
        // The compact source library needs a complete group header and card; dragging or Tab
        // already exposes the HUD underneath, so do not reserve an unusable blank footer here.
        bodyBottom = compact ? Math.min(screenHeight - 8 - panelPad, bodyTop + 126)
                : Math.min(screenHeight - 38, bodyTop + 220);
    }
    int toolbarWidth() { return (width - headerInset * 2 - toolbarGap * 3) / 4; }
    int toolbarX(int index) { return x + headerInset + index * (toolbarWidth() + toolbarGap); }
    int enableWidth() { return compact ? 50 : 78; }
    /**
     * 86 fits the full label ("模式：指尖"). A window that is short but wide (the user's 2560x1369 at GUI scale 5 is
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

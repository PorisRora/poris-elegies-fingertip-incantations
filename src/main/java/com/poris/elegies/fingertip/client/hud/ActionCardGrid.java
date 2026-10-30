package com.poris.elegies.fingertip.client.hud;

/** Pixel geometry shared by card placement and hit testing, including real non-interactive gutters. */
record ActionCardGrid(int x, int width, int columns, int cardWidth, int cardHeight, int gap) {
    static ActionCardGrid fit(int x, int width) {
        int columns = width >= 294 ? 3 : width >= 116 ? 2 : 1;
        int gap = 6;
        int cardWidth = Math.max(1, (width - (columns - 1) * gap) / columns);
        return new ActionCardGrid(x, width, columns, cardWidth, 48, gap);
    }
    int cardX(int column) { return x + column * (cardWidth + gap); }
    int rowPitch() { return cardHeight + gap; }
    int columnAt(double px) {
        if (px < x || px >= x + width) return -1;
        int column = (int)(px - x) / (cardWidth + gap);
        return column < columns && px < cardX(column) + cardWidth ? column : -1;
    }
}

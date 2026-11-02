package com.poris.elegies.fingertip.client.hud;

/** Pixel geometry for laying out cards: columns, card width and the gutters between them. */
record ActionCardGrid(int x, int width, int columns, int cardWidth, int cardHeight, int gap) {
    static ActionCardGrid fit(int x, int width) {
        int columns = width >= 294 ? 3 : width >= 116 ? 2 : 1;
        int gap = 6;
        int cardWidth = Math.max(1, (width - (columns - 1) * gap) / columns);
        return new ActionCardGrid(x, width, columns, cardWidth, 48, gap);
    }
    int cardX(int column) { return x + column * (cardWidth + gap); }
    int rowPitch() { return cardHeight + gap; }
}

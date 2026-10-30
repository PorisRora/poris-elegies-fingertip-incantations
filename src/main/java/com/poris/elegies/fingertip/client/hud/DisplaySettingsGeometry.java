package com.poris.elegies.fingertip.client.hud;

import java.util.List;

/** Floating controls choose a quiet corner; HUD coordinates are never projected into this panel. */
final class DisplaySettingsGeometry {
    record Rect(int x, int y, int width, int height) {
        int right() { return x + width; }
        int bottom() { return y + height; }
        boolean contains(Rect other) { return other.x >= x && other.y >= y && other.right() <= right() && other.bottom() <= bottom(); }
        int overlap(Rect other) {
            return Math.max(0, Math.min(right(), other.right()) - Math.max(x, other.x))
                    * Math.max(0, Math.min(bottom(), other.bottom()) - Math.max(y, other.y));
        }
    }

    final Rect panel, done, restore;

    DisplaySettingsGeometry(int screenWidth, int screenHeight, List<Rect> occupied) {
        int w = screenWidth < 440 ? 212 : 232;
        panel = corner(screenWidth, screenHeight, w, 188, occupied);
        restore = corner(screenWidth, screenHeight, 76, 18, occupied);
        done = new Rect(panel.right() - 66, panel.y + 159, 56, 18);
    }

    Rect theme(int index) { return choice(index, 42); }
    Rect range(int index) { return choice(index, 82); }
    Rect functions() { return new Rect(panel.x + 10, panel.y + 108, panel.width - 20, 18); }
    Rect durability() { return new Rect(panel.x + 10, panel.y + 130, panel.width - 20, 18); }
    /** The Tab hint, one or two lines, centred on the Done button beside it (2026-10-27: it sat 4 px high). */
    Rect hint(int lines) {
        int height = lines * 10 - 2;
        return new Rect(panel.x + 10, done.y + (done.height - height) / 2, panel.width - 86, height);
    }

    private Rect choice(int index, int y) {
        int w = (panel.width - 28) / 3;
        return new Rect(panel.x + 10 + index * (w + 4), panel.y + y, w, 18);
    }

    private static Rect corner(int width, int height, int w, int h, List<Rect> occupied) {
        int margin = 8;
        var choices = List.of(new Rect(width - w - margin, margin, w, h), new Rect(margin, margin, w, h),
                new Rect(width - w - margin, height - h - margin, w, h), new Rect(margin, height - h - margin, w, h));
        Rect best = choices.getFirst();
        int bestScore = Integer.MAX_VALUE;
        for (var candidate : choices) {
            int score = occupied.stream().mapToInt(candidate::overlap).sum();
            if (score < bestScore) { best = candidate; bestScore = score; }
        }
        return best;
    }
}

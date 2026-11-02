package com.poris.elegies.fingertip.client.hud;

import java.util.ArrayList;
import java.util.List;

/** Pixel layout math shared by arranging, group dragging and the auxiliary hand indicators. */
final class HudArrangement {
    static final int CELL = 20, DEFAULT_GAP = 4;
    record Point(int x, int y) {}
    record Bounds(int left, int top, int right, int bottom) {
        int width() { return right - left; }
        int height() { return bottom - top; }
    }

    static List<Point> positions(List<HudLayout.Slot> slots, int width, int height) {
        return slots.stream().map(s -> new Point(s.screenX(width, height), s.screenY(width, height))).toList();
    }

    static Bounds bounds(List<Point> points) {
        if (points.isEmpty()) return new Bounds(0, 0, 0, 0);
        return new Bounds(points.stream().mapToInt(Point::x).min().orElseThrow(),
                points.stream().mapToInt(Point::y).min().orElseThrow(),
                points.stream().mapToInt(Point::x).max().orElseThrow() + CELL,
                points.stream().mapToInt(Point::y).max().orElseThrow() + CELL);
    }

    /**
     * A single clamped delta preserves every internal gap, including at a screen edge. The range always holds 0, so a
     * selection already past an edge (1.1.4 shows what does not fit as it is) moves as usual (1.1.5, 2026-11-01, user:
     * "挤出去也按照正常挪"): an axis it was not moved along stays put, and no step takes a frame further off the screen,
     * while a step back in is taken as it is. The range used to leave 0 out there: Up on a row past the left edge also
     * pulled it 40 to the right (Regression in 1.1.4), and a selection longer than the screen made Math.clamp throw.
     */
    static List<Point> translate(List<Point> points, int dx, int dy, int width, int height) {
        if (points.isEmpty()) return points;
        var bounds = bounds(points);
        int x = Math.clamp(dx, Math.min(0, -bounds.left), Math.max(0, width - bounds.right));
        int y = Math.clamp(dy, Math.min(0, -bounds.top), Math.max(0, height - bounds.bottom));
        return points.stream().map(p -> new Point(p.x + x, p.y + y)).toList();
    }

    /** Nine columns normally; extra rows grow upward, without changing the order or any bindings. */
    static List<Point> arrange(List<Point> points, int gap, int width, int height) {
        if (points.isEmpty()) return points;
        int pitch = CELL + Math.clamp(gap, 0, 8);
        int maxColumns = Math.max(1, (width - 8 + gap) / pitch);
        int preferredRows = Math.max(1, (height - 84 + gap) / pitch);
        int columns = Math.min(maxColumns, Math.max(Math.min(9, points.size()), (points.size() + preferredRows - 1) / preferredRows));
        int rows = (points.size() + columns - 1) / columns;
        int gridWidth = columns * pitch - gap, gridHeight = rows * pitch - gap;
        var old = bounds(points);
        int left = Math.clamp((old.left + old.right - gridWidth) / 2, 4, Math.max(4, width - gridWidth - 4));
        int bottom = Math.clamp(old.bottom, gridHeight + 4, Math.max(gridHeight + 4, height - 4));
        var result = new ArrayList<Point>();
        for (int i = 0; i < points.size(); i++)
            result.add(new Point(left + i % columns * pitch, bottom - CELL - i / columns * pitch));
        return result;
    }

    /** A straight row/column has a fixed pitch; refuse overflow instead of wrapping or squeezing gaps. */
    static List<Point> line(List<Point> points, int gap, int width, int height, boolean horizontal) {
        if (points.isEmpty()) return points;
        int pitch = CELL + Math.clamp(gap, 0, 8);
        int length = CELL + (points.size() - 1) * pitch;
        int w = horizontal ? length : CELL, h = horizontal ? CELL : length;
        // Between the side margins and below the toolbar band, across as well as along (a very short or narrow window).
        if (w > width - 8 || h > height - 84) return List.of();
        var old = bounds(points);
        int left = Math.clamp(old.left(), 4, width - w - 4);
        int top = Math.clamp(old.top(), 80, height - h - 4);
        var result = new ArrayList<Point>();
        for (int i = 0; i < points.size(); i++) result.add(new Point(left + (horizontal ? i * pitch : 0), top + (horizontal ? 0 : i * pitch)));
        return result;
    }

    static boolean free(Point p, List<Point> occupied, int gap, int width, int height) {
        return HudPlacement.firstFree(List.of(HudPlacement.Rect.cell(p.x, p.y)), HudPlacement.Rect.cells(occupied), gap, width, height) != null;
    }

    /** Follow the moved HUD, rather than colliding with its wider row at vanilla's fixed coordinates. */
    static Point auxiliary(List<Point> points, int side, int width, int height) {
        return auxiliary(points, points, side, width, height);
    }
    /**
     * A free spot beside the box of {@code anchor}, clear of everything in {@code occupied}. The two differ when a
     * frame placed on its own, like the off hand, must be avoided without stretching the box it sits away from.
     */
    static Point auxiliary(List<Point> anchor, List<Point> occupied, int side, int width, int height) {
        var box = bounds(anchor);
        int left = box.left - CELL - DEFAULT_GAP, right = box.right + DEFAULT_GAP;
        // Beside the box on the asked side, then the other side, above, below; then the screen row by row from the bottom.
        var candidates = new java.util.ArrayList<>(List.of(HudPlacement.Rect.cell(side < 0 ? left : right, box.bottom - CELL),
                HudPlacement.Rect.cell(side < 0 ? right : left, box.bottom - CELL),
                HudPlacement.Rect.cell(box.left, box.top - CELL - DEFAULT_GAP),
                HudPlacement.Rect.cell(box.left, box.bottom + DEFAULT_GAP)));
        for (int y = height - CELL - 2; y >= 0; y -= CELL + DEFAULT_GAP)
            for (int x = 2; x + CELL <= width; x += CELL + DEFAULT_GAP) candidates.add(HudPlacement.Rect.cell(x, y));
        var spot = HudPlacement.firstFree(candidates, HudPlacement.Rect.cells(occupied), 2, width, height);
        return spot == null ? null : new Point(spot.x(), spot.y());
    }
    private HudArrangement() {}
}

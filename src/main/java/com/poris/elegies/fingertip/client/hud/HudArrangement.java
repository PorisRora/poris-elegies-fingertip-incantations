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
        return slots.stream().map(s -> new Point(s.screenX(width), s.screenY(height))).toList();
    }

    static Bounds bounds(List<Point> points) {
        if (points.isEmpty()) return new Bounds(0, 0, 0, 0);
        return new Bounds(points.stream().mapToInt(Point::x).min().orElseThrow(),
                points.stream().mapToInt(Point::y).min().orElseThrow(),
                points.stream().mapToInt(Point::x).max().orElseThrow() + CELL,
                points.stream().mapToInt(Point::y).max().orElseThrow() + CELL);
    }

    /** A single clamped delta preserves every internal gap, including at a screen edge. */
    static List<Point> translate(List<Point> points, int dx, int dy, int width, int height) {
        if (points.isEmpty()) return points;
        var bounds = bounds(points);
        int x = Math.clamp(dx, -bounds.left, width - bounds.right);
        int y = Math.clamp(dy, -bounds.top, height - bounds.bottom);
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

    static void apply(List<HudLayout.Slot> slots, List<Point> points, int width, int height) {
        for (int i = 0; i < slots.size(); i++) {
            slots.get(i).x = points.get(i).x - width / 2;
            slots.get(i).y = points.get(i).y - height;
        }
    }

    /** A straight row/column has a fixed pitch; refuse overflow instead of wrapping or squeezing gaps. */
    static List<Point> line(List<Point> points, int gap, int width, int height, boolean horizontal) {
        if (points.isEmpty()) return points;
        int pitch = CELL + Math.clamp(gap, 0, 8);
        int length = CELL + (points.size() - 1) * pitch;
        if (length > (horizontal ? width - 8 : height - 84)) return List.of();
        var old = bounds(points);
        int w = horizontal ? length : CELL, h = horizontal ? CELL : length;
        int left = Math.clamp(old.left(), 4, width - w - 4);
        int top = Math.clamp(old.top(), 80, height - h - 4);
        var result = new ArrayList<Point>();
        for (int i = 0; i < points.size(); i++) result.add(new Point(left + (horizontal ? i * pitch : 0), top + (horizontal ? 0 : i * pitch)));
        return result;
    }

    static boolean free(Point p, List<Point> occupied, int gap, int width, int height) {
        if (p.x < 0 || p.y < 0 || p.x + CELL > width || p.y + CELL > height) return false;
        return occupied.stream().noneMatch(q -> p.x < q.x + CELL + gap && p.x + CELL + gap > q.x
                && p.y < q.y + CELL + gap && p.y + CELL + gap > q.y);
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
        for (var p : List.of(new Point(side < 0 ? left : right, box.bottom - CELL),
                new Point(side < 0 ? right : left, box.bottom - CELL),
                new Point(box.left, box.top - CELL - DEFAULT_GAP),
                new Point(box.left, box.bottom + DEFAULT_GAP)))
            if (free(p, occupied, 2, width, height)) return p;
        for (int y = height - CELL - 2; y >= 0; y -= CELL + DEFAULT_GAP)
            for (int x = 2; x + CELL <= width; x += CELL + DEFAULT_GAP) {
                var p = new Point(x, y);
                if (free(p, occupied, 2, width, height)) return p;
            }
        return null;
    }
    private HudArrangement() {}
}

package com.poris.elegies.fingertip.client.hud;

import java.util.List;

/**
 * Magnetic alignment. One delta moves the entire selection, so a native row never splits. A slot lines up with the column
 * or row of any other slot however far apart; sitting one gap
 * beside a slot only snaps next to it. On a tie the nearer slot wins.
 */
final class HudSnap {
    private static final int RADIUS = 4;
    record Result(List<HudArrangement.Point> points, Integer guideX, Integer guideY) {}
    private record Axis(int delta, int coordinate, int distance) {}

    static Result align(List<HudArrangement.Point> moving, List<HudArrangement.Point> fixed,
                        int gap, int width, int height) {
        Axis x = null, y = null;
        int pitch = HudArrangement.CELL + Math.clamp(gap, 0, 8);
        for (var p : moving) for (var q : fixed) {
            int distance = Math.abs(p.x() - q.x()) + Math.abs(p.y() - q.y());
            x = nearer(x, q.x() - p.x(), q.x(), distance);
            y = nearer(y, q.y() - p.y(), q.y(), distance);
            for (int offset : new int[]{-pitch, pitch}) {
                if (Math.abs(p.y() - q.y()) <= pitch + RADIUS)
                    x = nearer(x, q.x() + offset - p.x(), q.x() + offset, distance);
                if (Math.abs(p.x() - q.x()) <= pitch + RADIUS)
                    y = nearer(y, q.y() + offset - p.y(), q.y() + offset, distance);
            }
        }
        var both = shifted(moving, x, y);
        if (valid(both, fixed, width, height)) return result(both, x, y);
        // Do not manufacture an overlap or push a group past the viewport to satisfy a guide.
        var horizontal = shifted(moving, x, null);
        if (x != null && valid(horizontal, fixed, width, height)) return result(horizontal, x, null);
        var vertical = shifted(moving, null, y);
        if (y != null && valid(vertical, fixed, width, height)) return result(vertical, null, y);
        return new Result(moving, null, null);
    }
    private static Axis nearer(Axis old, int delta, int coordinate, int distance) {
        if (Math.abs(delta) > RADIUS) return old;
        if (old == null || Math.abs(delta) < Math.abs(old.delta)
                || Math.abs(delta) == Math.abs(old.delta) && distance < old.distance)
            return new Axis(delta, coordinate, distance);
        return old;
    }
    private static List<HudArrangement.Point> shifted(List<HudArrangement.Point> points, Axis x, Axis y) {
        int dx = x == null ? 0 : x.delta, dy = y == null ? 0 : y.delta;
        return points.stream().map(p -> new HudArrangement.Point(p.x() + dx, p.y() + dy)).toList();
    }
    private static boolean valid(List<HudArrangement.Point> points, List<HudArrangement.Point> fixed, int w, int h) {
        return points.stream().allMatch(p -> HudArrangement.free(p, fixed, 0, w, h));
    }
    private static Result result(List<HudArrangement.Point> points, Axis x, Axis y) {
        return new Result(points, x == null ? null : x.coordinate, y == null ? null : y.coordinate);
    }
    private HudSnap() {}
}

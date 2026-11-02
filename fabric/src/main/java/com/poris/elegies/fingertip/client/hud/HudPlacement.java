package com.poris.elegies.fingertip.client.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 指尖定位法 (1.1.0; 2026-11-01): where slot frames and
 * the small floating controls go on a screen of any size.
 * <ul>
 * <li>Frames at most {@link #TOUCH} GUI pixels apart, edge to edge, that share a point form a group (a lone frame is a
 * group of one). A frame joins a group when the player puts it down there in the editor, never when the arrow keys or
 * a small window bring them together (1.1.6, {@link #commit}).</li>
 * <li>Each group keeps its distance to one of nine points (九宫点: the corners, the edge midpoints and the centre),
 * the one of the third of the screen its centre is moved into; so it follows its corner or edge when the window
 * changes, and its gaps never change.</li>
 * <li>What does not fit is shown as it is, past the edge or over another group, and the player sorts it out (1.1.4,
 * 2026-11-01). 1.1.0 to 1.1.3 pushed groups back in and made them yield, which let
 * what was shown drift from what was stored; now what is shown is what is stored.</li>
 * <li>A small control takes the first spot in its own list of candidates that covers nothing ({@link #firstFree}).</li>
 * </ul>
 * Every slot used to be clamped into the screen on its own, from the bottom centre, so in a narrower window the slots
 * past the edge piled up on one spot (2026-11-01).
 */
final class HudPlacement {
    static final int CELL = HudArrangement.CELL;
    /** Edge-to-edge gap at which two frames touch: the widest spacing an arrangement can use, so one never splits. */
    static final int TOUCH = 8;

    /** The nine points; {@code ax} and {@code ay} are 0 for left or top, 1 for the middle, 2 for right or bottom. */
    enum Point9 {
        TOP_LEFT(0, 0, "top_left"), TOP(1, 0, "top"), TOP_RIGHT(2, 0, "top_right"),
        LEFT(0, 1, "left"), CENTER(1, 1, "center"), RIGHT(2, 1, "right"),
        BOTTOM_LEFT(0, 2, "bottom_left"), BOTTOM(1, 2, "bottom"), BOTTOM_RIGHT(2, 2, "bottom_right");
        final int ax, ay;
        final String id;
        Point9(int ax, int ay, String id) { this.ax = ax; this.ay = ay; this.id = id; }
        int x(int width) { return ax == 0 ? 0 : ax == 1 ? width / 2 : width; }
        int y(int height) { return ay == 0 ? 0 : ay == 1 ? height / 2 : height; }
        static Point9 of(int ax, int ay) { return values()[ay * 3 + ax]; }
        /** The point a layout file names, or null for a missing or unknown name. */
        static Point9 from(String id) {
            if (id != null) for (var point : values()) if (point.id.equals(id)) return point;
            return null;
        }
        /** The point for a group whose box runs from {@code left} to {@code right} and {@code top} to {@code bottom}. */
        static Point9 nearest(int left, int top, int right, int bottom, int width, int height) {
            return of(third(left + right, width), third(top + bottom, height));
        }
        /** 0, 1 or 2: the third of {@code size} that the midpoint of a span (given doubled) falls in. */
        private static int third(int doubledMid, int size) { return doubledMid * 3 < size * 2 ? 0 : doubledMid * 3 > size * 4 ? 2 : 1; }
    }

    record Rect(int x, int y, int width, int height) {
        int right() { return x + width; }
        int bottom() { return y + height; }
        boolean contains(Rect other) { return other.x >= x && other.y >= y && other.right() <= right() && other.bottom() <= bottom(); }
        int overlap(Rect other) {
            return Math.max(0, Math.min(right(), other.right()) - Math.max(x, other.x))
                    * Math.max(0, Math.min(bottom(), other.bottom()) - Math.max(y, other.y));
        }
        /** Closer than {@code margin} on both axes (overlapping counts too). */
        boolean near(Rect other, int margin) {
            return x < other.right() + margin && other.x < right() + margin && y < other.bottom() + margin && other.y < bottom() + margin;
        }
        static Rect cell(int x, int y) { return new Rect(x, y, CELL, CELL); }
        static List<Rect> cells(List<HudArrangement.Point> points) { return points.stream().map(p -> cell(p.x(), p.y())).toList(); }
    }

    /** A frame as stored: its point and offset (no point: never placed). */
    record Frame(Point9 point, int dx, int dy) {}
    /** Where each frame is, by its index in the frame list. */
    record Result(int[] x, int[] y) {}

    /** Two frames at these top-left corners touch (or overlap). */
    static boolean touching(int ax, int ay, int bx, int by) {
        return Math.max(bx - ax - CELL, ax - bx - CELL) <= TOUCH && Math.max(by - ay - CELL, ay - by - CELL) <= TOUCH;
    }

    /**
     * Where the frames are on a width × height screen: each at its point plus its offset. A frame in {@code fixed} (the
     * slots being dragged) is drawn where it is held. Nothing is pushed back in or moved out of the way.
     */
    static Result resolve(List<Frame> frames, int width, int height, Map<Integer, HudArrangement.Point> fixed) {
        int n = frames.size();
        int[] x = new int[n], y = new int[n];
        for (int i = 0; i < n; i++) {
            var held = fixed.get(i);
            if (held != null) { x[i] = held.x(); y[i] = held.y(); continue; }
            var frame = frames.get(i);
            x[i] = frame.point().x(width) + frame.dx(); y[i] = frame.point().y(height) + frame.dy();
        }
        return new Result(x, y);
    }
    static Rect box(int[] members, int[] x, int[] y) {
        int left = Integer.MAX_VALUE, top = Integer.MAX_VALUE, right = Integer.MIN_VALUE, bottom = Integer.MIN_VALUE;
        for (int m : members) {
            left = Math.min(left, x[m]); top = Math.min(top, y[m]);
            right = Math.max(right, x[m] + CELL); bottom = Math.max(bottom, y[m] + CELL);
        }
        return new Rect(left, top, right - left, bottom - top);
    }

    /**
     * How the frames an edit moved got there (1.1.6, 2026-11-01). Only frames put down join a group, never frames nudged
     * (, "1、在编辑器里的时候 2、玩家添加/拖动放置的时候").
     */
    enum Edit {
        /** Dragged and let go, or a new frame placed. */
        DROP,
        /** Moved by the arrow keys. */
        NUDGE,
        /** Lined up by the toolbar: the frames lined up become one group. */
        ARRANGE
    }

    /**
     * Records an edit on a width × height screen, where what is shown is what is stored. {@code before} is where every
     * frame was and {@code points} where it is now; {@code changed} holds the frames the player moved or placed, and only
     * those are stored again: every other group keeps its point and offsets.
     * <ul>
     * <li>Frames moved together keep the groups they were in; they move as one piece, so frames a small window shows on
     * top of each other stay apart. A frame never placed goes with the moved frames it touches, and frames lined up
     * ({@link Edit#ARRANGE}) are one group.</li>
     * <li>A group of them put down ({@link Edit#DROP}, {@link Edit#ARRANGE}) within {@link #TOUCH} of a frame that did not
     * move joins that frame's group and takes its point ({@link #landedOn}).</li>
     * <li>Otherwise, put down 0 to {@link #TOUCH} pixels from one screen edge and not that close to the opposite one, it
     * follows that edge on that axis ({@link #edge}; 1.2.0, 2026-11-01); its other end may run
     * past the screen. Taken off the edge it was against before the edit, the third it is in decides again on that axis
     * (review of 1.2.0: a row put against the left edge and dragged to the middle kept the left edge).</li>
     * <li>Otherwise, and always when nudged, it keeps its point along an axis on which its box is still in the same third
     * and takes the point of the third its box is in now on the other. Offsets are measured from the point.</li>
     * </ul>
     * Returns the new frame for each moved index. (1.1.6, 2026-11-01.
     * Regression in 1.1.5: a row nudged in a small window over a group it already lay on was joined to it when one
     * more pair of frames touched, and landed at x 1132 in the big window instead of 172. Before, frames touching after an
     * edit were joined unless that pair had touched before, which could also re-pick the point of a group not moved.)
     */
    static Map<Integer, Frame> commit(List<HudArrangement.Point> points, List<HudArrangement.Point> before, List<Frame> previous,
                                      Set<Integer> changed, Edit edit, int width, int height) {
        int n = points.size();
        int[] x = new int[n], y = new int[n], bx = new int[n], by = new int[n];
        for (int i = 0; i < n; i++) {
            x[i] = points.get(i).x(); y[i] = points.get(i).y(); bx[i] = before.get(i).x(); by[i] = before.get(i).y();
        }
        var moved = new boolean[n];
        for (int i : changed) moved[i] = true;
        var unions = new Unions(n);
        for (int i : changed) for (int j : changed) {
            if (i >= j) continue;
            var a = previous.get(i).point(); var b = previous.get(j).point();
            boolean together = edit == Edit.ARRANGE
                    || (a == null || b == null ? touching(x[i], y[i], x[j], y[j]) : a == b && touching(bx[i], by[i], bx[j], by[j]));
            if (together) unions.join(i, j);
        }
        var result = new java.util.HashMap<Integer, Frame>();
        for (int[] members : unions.sets(moved)) {
            var point = edit == Edit.NUDGE ? null : landedOn(members, x, y, moved, previous);
            if (point == null) {
                var box = box(members, x, y);
                var nearest = Point9.nearest(box.x(), box.y(), box.right(), box.bottom(), width, height);
                var was = box(members, bx, by);
                var nearestBefore = Point9.nearest(was.x(), was.y(), was.right(), was.bottom(), width, height);
                int ax = nearest.ax == nearestBefore.ax ? shared(members, previous, true) : -1;
                int ay = nearest.ay == nearestBefore.ay ? shared(members, previous, false) : -1;
                if (edit != Edit.NUDGE) {
                    int ex = edge(box.x(), width - box.right()), ey = edge(box.y(), height - box.bottom());
                    if (ex >= 0 || edge(was.x(), width - was.right()) >= 0) ax = ex;
                    if (ey >= 0 || edge(was.y(), height - was.bottom()) >= 0) ay = ey;
                }
                point = Point9.of(ax >= 0 ? ax : nearest.ax, ay >= 0 ? ay : nearest.ay);
            }
            for (int m : members) result.put(m, new Frame(point, x[m] - point.x(width), y[m] - point.y(height)));
        }
        return result;
    }
    /**
     * The point of the group {@code members} were put down on: of the frames that did not move, the nearest within
     * {@link #TOUCH}, edge to edge, and of those lying on them the one drawn last, the later in the list; null if none
     * (, "如果多组挨在一起那就分给最上层的那组").
     */
    private static Point9 landedOn(int[] members, int[] x, int[] y, boolean[] moved, List<Frame> previous) {
        int best = -1, bestGap = TOUCH;
        for (int f = 0; f < x.length; f++) {
            if (moved[f] || previous.get(f).point() == null) continue;
            int gap = Integer.MAX_VALUE;
            for (int m : members) gap = Math.min(gap, Math.max(0, Math.max(Math.abs(x[f] - x[m]), Math.abs(y[f] - y[m])) - CELL));
            if (gap <= bestGap) { best = f; bestGap = gap; }
        }
        return best < 0 ? null : previous.get(best).point();
    }
    /**
     * The edge a box keeps to on one axis, from its gaps to the near and the far edge: 0 when the near gap is 0 to
     * {@link #TOUCH} and the far one is not, 2 the other way round, else -1 (against neither or both; a box past an edge
     * is not against it, and a row as wide as the screen keeps to its third). Only the side against the edge counts, so
     * a row from the left edge that runs past the right one follows the left edge. A long row against the left edge, its
     * centre in the middle third, then stays there in a narrower window instead of following the middle off it.
     */
    private static int edge(int near, int far) {
        boolean a = near >= 0 && near <= TOUCH, b = far >= 0 && far <= TOUCH;
        return a == b ? -1 : a ? 0 : 2;
    }
    /** The column ({@code across}) or row of the stored point that every member with one shares, or -1. */
    private static int shared(int[] members, List<Frame> previous, boolean across) {
        int found = -1;
        for (int m : members) {
            var point = previous.get(m).point();
            if (point == null) continue;
            int value = across ? point.ax : point.ay;
            if (found >= 0 && found != value) return -1;
            found = value;
        }
        return found;
    }

    /** The first candidate wholly on the screen that comes no closer than {@code margin} to anything in {@code avoid}, or null. */
    static Rect firstFree(List<Rect> candidates, List<Rect> avoid, int margin, int width, int height) {
        var screen = new Rect(0, 0, width, height);
        for (var candidate : candidates) {
            if (!screen.contains(candidate)) continue;
            boolean clear = true;
            for (var other : avoid) if (candidate.near(other, margin)) { clear = false; break; }
            if (clear) return candidate;
        }
        return null;
    }
    /** {@link #firstFree}, or else the candidate on the screen covering the least of {@code avoid} (the earliest of equals). */
    static Rect bestSpot(List<Rect> candidates, List<Rect> avoid, int margin, int width, int height) {
        var free = firstFree(candidates, avoid, margin, width, height);
        if (free != null) return free;
        var screen = new Rect(0, 0, width, height);
        Rect best = null;
        int least = Integer.MAX_VALUE;
        for (var candidate : candidates) {
            if (!screen.contains(candidate)) continue;
            int covered = 0;
            for (var other : avoid) covered += candidate.overlap(other);
            if (covered < least) { best = candidate; least = covered; }
        }
        return best != null || candidates.isEmpty() ? best : candidates.getFirst();
    }

    /** Disjoint sets of indices, listed by their lowest member. */
    private static final class Unions {
        private final int[] parent;
        Unions(int n) { parent = new int[n]; for (int i = 0; i < n; i++) parent[i] = i; }
        int find(int i) { while (parent[i] != i) { parent[i] = parent[parent[i]]; i = parent[i]; } return i; }
        void join(int a, int b) { int ra = find(a), rb = find(b); if (ra != rb) parent[Math.max(ra, rb)] = Math.min(ra, rb); }
        List<int[]> sets(boolean[] include) {
            var byRoot = new java.util.LinkedHashMap<Integer, List<Integer>>();
            for (int i = 0; i < parent.length; i++) if (include[i]) byRoot.computeIfAbsent(find(i), k -> new ArrayList<>()).add(i);
            var result = new ArrayList<int[]>();
            for (var members : byRoot.values()) result.add(members.stream().mapToInt(Integer::intValue).toArray());
            return result;
        }
    }
    private HudPlacement() {}
}

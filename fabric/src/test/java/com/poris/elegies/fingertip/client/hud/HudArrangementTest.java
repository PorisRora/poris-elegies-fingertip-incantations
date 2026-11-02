package com.poris.elegies.fingertip.client.hud;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class HudArrangementTest {
    @Test void rowsAndColumnsKeepPitchAndRefuseOverflow() {
        var original = List.of(new HudArrangement.Point(280, 180), new HudArrangement.Point(280, 100), new HudArrangement.Point(100, 160));
        var row = HudArrangement.line(original, 4, 320, 200, true);
        var column = HudArrangement.line(original, 4, 320, 200, false);
        for (int i = 1; i < 3; i++) {
            assertEquals(24, row.get(i).x() - row.get(i - 1).x()); assertEquals(row.get(0).y(), row.get(i).y());
            assertEquals(24, column.get(i).y() - column.get(i - 1).y()); assertEquals(column.get(0).x(), column.get(i).x());
        }
        assertTrue(HudArrangement.bounds(row).right() <= 316); assertTrue(HudArrangement.bounds(column).bottom() <= 196);
        assertTrue(HudArrangement.line(scattered(64, 320, 200), 4, 320, 200, true).isEmpty());
        assertTrue(HudArrangement.line(scattered(9, 320, 200), 4, 320, 200, false).isEmpty());
    }
    private static List<HudArrangement.Point> scattered(int count, int width, int height) {
        var result = new ArrayList<HudArrangement.Point>();
        for (int i = 0; i < count; i++) result.add(new HudArrangement.Point(width / 2 - 90 + i % 9 * 20, height - 22 - i / 9 * 20));
        return result;
    }

    @Test void arrangeKeepsUniformGapsAndNoOverlapsAcrossWindowSizes() {
        for (int[] size : new int[][]{{320, 200}, {427, 267}, {640, 400}})
            for (int count : new int[]{1, 9, 15, 64})
                for (int gap : new int[]{0, 2, 4, 6, 8}) {
                    var points = HudArrangement.arrange(scattered(count, size[0], size[1]), gap, size[0], size[1]);
                    assertEquals(count, points.size());
                    for (int i = 0; i < count; i++) {
                        var others = new ArrayList<>(points); var p = others.remove(i);
                        assertTrue(HudArrangement.free(p, others, gap, size[0], size[1]), count + " slots, gap " + gap);
                        if (i > 0 && p.y() == points.get(i - 1).y()) assertEquals(20 + gap, p.x() - points.get(i - 1).x());
                    }
                    assertEquals(points, HudArrangement.arrange(points, gap, size[0], size[1]), "arrange must not creep on repeated clicks");
                }
    }

    @Test void fifteenSlotsUseNineBelowAndSixAbove() {
        var points = HudArrangement.arrange(scattered(15, 640, 400), 4, 640, 400);
        assertEquals(points.get(0).x(), points.get(9).x());
        assertEquals(24, points.get(0).y() - points.get(9).y());
        assertEquals(8 * 24, points.get(8).x() - points.get(0).x());
    }

    @Test void groupClampingPreservesEveryGapAtEachScreenEdge() {
        var original = List.of(new HudArrangement.Point(100, 70), new HudArrangement.Point(124, 70), new HudArrangement.Point(100, 46));
        for (int[] delta : new int[][]{{999, 999}, {-999, -999}, {999, -999}, {-999, 999}, {7, -13}}) {
            var moved = HudArrangement.translate(original, delta[0], delta[1], 320, 200);
            for (int i = 0; i < moved.size(); i++) {
                assertEquals(original.get(i).x() - original.get(0).x(), moved.get(i).x() - moved.get(0).x());
                assertEquals(original.get(i).y() - original.get(0).y(), moved.get(i).y() - moved.get(0).y());
                assertTrue(moved.get(i).x() >= 0 && moved.get(i).x() <= 300 && moved.get(i).y() >= 0 && moved.get(i).y() <= 180);
            }
        }
    }

    @Test void auxiliaryHandIndicatorsNeverOverlapExpandedOrMovedSlots() {
        var original = HudArrangement.arrange(scattered(15, 320, 200), 4, 320, 200);
        for (int[] delta : new int[][]{{0, 0}, {-999, 0}, {999, 0}, {0, -999}}) {
            var occupied = new ArrayList<>(HudArrangement.translate(original, delta[0], delta[1], 320, 200));
            for (int side : new int[]{-1, 1}) {
                var position = HudArrangement.auxiliary(occupied, side, 320, 200);
                assertNotNull(position);
                assertTrue(HudArrangement.free(position, occupied, 2, 320, 200));
                occupied.add(position);
            }
        }
    }

    @Test void attackIndicatorFollowsTheHudAndOnlyAvoidsTheOffhandFrame() {
        var row = scattered(9, 640, 400);
        var beside = HudArrangement.auxiliary(row, 1, 640, 400);
        assertNotNull(beside);
        // An off-hand frame dragged far away is avoided, but must not stretch the box the indicator stays beside.
        var far = new ArrayList<>(row); far.add(new HudArrangement.Point(4, 4));
        assertEquals(beside, HudArrangement.auxiliary(row, far, 1, 640, 400));
        // Parked exactly where the indicator would go, the frame moves it to another free place instead of overlapping.
        var blocked = new ArrayList<>(row); blocked.add(beside);
        var moved = HudArrangement.auxiliary(row, blocked, 1, 640, 400);
        assertNotNull(moved);
        assertNotEquals(beside, moved);
        assertTrue(HudArrangement.free(moved, blocked, 2, 640, 400));
    }

    @Test void aSelectionLongerThanTheScreenNeverGoesFurtherOff() {
        // A row laid out in a big window and selected whole in a small one: dragging or the arrow keys made Math.clamp
        // throw (2026-11-01, reproduction). Along the long axis it moves only until its end on the screen meets the edge
        // (1.1.5), and the end already past it never goes further; across it, it moves as usual.
        var row = new ArrayList<HudArrangement.Point>();
        for (int i = 0; i < 20; i++) row.add(new HudArrangement.Point(4 + 24 * i, 180));
        var moved = HudArrangement.translate(row, -30, -10, 320, 240);
        for (int i = 0; i < row.size(); i++) { assertEquals(row.get(i).x() - 4, moved.get(i).x()); assertEquals(170, moved.get(i).y()); }
        assertEquals(row, HudArrangement.translate(row, 30, 0, 320, 240), "its right end is past the edge already");
        var column = new ArrayList<HudArrangement.Point>();
        for (int i = 0; i < 12; i++) column.add(new HudArrangement.Point(100, 4 + 24 * i));
        var shifted = HudArrangement.translate(column, 10, 25, 320, 240);
        for (int i = 0; i < column.size(); i++) { assertEquals(110, shifted.get(i).x()); assertEquals(column.get(i).y(), shifted.get(i).y()); }
    }

    @Test void aGroupPartlyPastAnEdgeMovesOnlyWhereItIsMoved() {
        // 1.1.4 shows what does not fit as it is, and such a group moves as usual (1.1.5).
        // Regression in 1.1.4: a short row past the left edge, moved only up, was also pulled 40 to the right.
        var row = new ArrayList<HudArrangement.Point>();
        for (int i = 0; i < 4; i++) row.add(new HudArrangement.Point(-40 + 24 * i, 174));
        var up = HudArrangement.translate(row, 0, -1, 320, 240);
        for (int i = 0; i < row.size(); i++) { assertEquals(row.get(i).x(), up.get(i).x(), "Up leaves x alone"); assertEquals(173, up.get(i).y()); }
        assertEquals(-39, HudArrangement.translate(row, 1, 0, 320, 240).getFirst().x(), "Right brings it in by one, not 40");
        assertEquals(row, HudArrangement.translate(row, -1, 0, 320, 240), "Left would take it further off");
        // Past the top, Right leaves y alone and Down brings it in by one.
        var column = new ArrayList<HudArrangement.Point>();
        for (int i = 0; i < 4; i++) column.add(new HudArrangement.Point(100, -30 + 24 * i));
        var right = HudArrangement.translate(column, 1, 0, 320, 240);
        for (int i = 0; i < column.size(); i++) { assertEquals(101, right.get(i).x()); assertEquals(column.get(i).y(), right.get(i).y(), "Right leaves y alone"); }
        assertEquals(-29, HudArrangement.translate(column, 0, 1, 320, 240).getFirst().y());
    }

    @Test void aRowNeedsRoomAcrossAsWellAsAlong() {
        var two = List.of(new HudArrangement.Point(10, 10), new HudArrangement.Point(40, 10));
        assertTrue(HudArrangement.line(two, 4, 320, 100, true).isEmpty(), "no room for a row below the toolbar band");
        assertTrue(HudArrangement.line(two, 4, 20, 240, false).isEmpty(), "no room for a column between the side margins");
        assertEquals(2, HudArrangement.line(two, 4, 320, 240, true).size());
    }
}

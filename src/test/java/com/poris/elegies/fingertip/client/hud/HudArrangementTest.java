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
}

package com.poris.elegies.fingertip.client.hud;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

class HudSnapTest {
    private HudArrangement.Point p(int x, int y) { return new HudArrangement.Point(x, y); }
    @Test void nearbySlotGetsBothColumnAndGapWithoutMovingItsNeighbor() {
        var fixed = List.of(p(100, 100));
        var result = HudSnap.align(List.of(p(103, 127)), fixed, 4, 320, 200);
        assertEquals(List.of(p(100, 124)), result.points());
        assertEquals(List.of(p(100, 100)), fixed);
        assertNotNull(result.guideX()); assertNotNull(result.guideY());
    }
    @Test void currentGapIsUsedAndDistantSlotsOnlyAlign() {
        assertEquals(List.of(p(128, 100)), HudSnap.align(List.of(p(131, 102)), List.of(p(100, 100)), 8, 320, 200).points());
        // Far below: same column, no row (2026-10-29: alignment is not only for slots that touch).
        var column = HudSnap.align(List.of(p(103, 180)), List.of(p(100, 100)), 4, 320, 200);
        assertEquals(List.of(p(100, 180)), column.points()); assertEquals(100, column.guideX()); assertNull(column.guideY());
        // Far to the right: same row.
        var row = HudSnap.align(List.of(p(250, 97)), List.of(p(100, 100)), 4, 320, 200);
        assertEquals(List.of(p(250, 100)), row.points()); assertNull(row.guideX()); assertEquals(100, row.guideY());
        // One gap beside a slot only snaps next to it, not from far away.
        var beside = List.of(p(127, 180));
        assertEquals(beside, HudSnap.align(beside, List.of(p(100, 100)), 4, 320, 200).points());
    }
    @Test void theNearerSlotWinsATie() {
        // Columns 100 and 106 are both 3 away; the slot right above wins over the far one.
        var result = HudSnap.align(List.of(p(103, 130)), List.of(p(106, 106), p(100, 20)), 4, 320, 200);
        assertEquals(106, result.points().getFirst().x());
    }
    @Test void nativeRowMovesRigidlyAndStopsSnappingOnceDraggedAway() {
        var row = IntStream.range(0, 9).mapToObj(i -> p(20 + i * 24, 127)).toList();
        var result = HudSnap.align(row, List.of(p(20, 100)), 4, 320, 200);
        assertEquals(124, result.points().getFirst().y());
        for (int i = 1; i < 9; i++) {
            assertEquals(24, result.points().get(i).x() - result.points().get(i - 1).x());
            assertEquals(124, result.points().get(i).y());
        }
        var free = HudArrangement.translate(row, 9, 10, 320, 200);
        assertEquals(free, HudSnap.align(free, List.of(p(20, 100)), 4, 320, 200).points());
    }
    @Test void snappingNeverCreatesAnOverlapOrCrossesScreenEdges() {
        var points = List.of(p(30, 100), p(298, 100));
        var result = HudSnap.align(points, List.of(p(33, 124)), 4, 320, 200);
        for (var point : result.points()) assertTrue(point.x() + 20 <= 320);
        assertEquals(268, result.points().get(1).x() - result.points().getFirst().x());
        var overlap = List.of(p(99, 99));
        var refused = HudSnap.align(overlap, List.of(p(100, 100)), 4, 320, 200);
        assertEquals(overlap, refused.points()); assertNull(refused.guideX()); assertNull(refused.guideY());
    }
}

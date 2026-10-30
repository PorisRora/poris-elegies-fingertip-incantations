package com.poris.elegies.fingertip.client.hud;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ActionCardGridTest {
    @Test void sidebarsPreserveReadableNamesAndWideLibrariesAllowThreeColumns() {
        var normal = ActionCardGrid.fit(20, 214 - 14);
        var compact = ActionCardGrid.fit(20, 152 - 14);
        var wide = ActionCardGrid.fit(20, 300);
        assertEquals(2, normal.columns()); assertEquals(2, compact.columns()); assertEquals(3, wide.columns());
        assertTrue(normal.cardWidth() >= 90, "normal source names have space without excessive line breaks");
        for (var grid : new ActionCardGrid[]{normal, compact, wide}) {
            assertTrue(grid.cardWidth() >= 60, "each card has space for a full icon and wrapped text");
            for (int column = 0; column < grid.columns(); column++) {
                assertEquals(column, grid.columnAt(grid.cardX(column) + grid.cardWidth() / 2.0));
                assertTrue(grid.cardX(column) + grid.cardWidth() <= grid.x() + grid.width());
            }
        }
    }
    @Test void guttersAndOutsideMarginsNeverSelectANeighboringCard() {
        for (int width : new int[]{138, 200, 280}) {
            var grid = ActionCardGrid.fit(17, width);
            assertEquals(-1, grid.columnAt(16.9)); assertEquals(-1, grid.columnAt(17 + width));
            for (int column = 0; column + 1 < grid.columns(); column++) {
                int end = grid.cardX(column) + grid.cardWidth();
                assertEquals(-1, grid.columnAt(end));
                assertEquals(-1, grid.columnAt(end + grid.gap() / 2.0));
                assertEquals(column + 1, grid.columnAt(grid.cardX(column + 1)));
            }
        }
    }
}

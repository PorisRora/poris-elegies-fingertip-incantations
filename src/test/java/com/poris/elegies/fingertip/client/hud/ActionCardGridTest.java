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
                assertTrue(grid.cardX(column) + grid.cardWidth() <= grid.x() + grid.width());
            }
        }
    }
}

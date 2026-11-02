package com.poris.elegies.fingertip.client.hud;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LayoutToolbarGeometryTest {
    @Test void groupsStayInsideThePanelWithoutOverlappingAcrossGuiSizes() {
        for (int width : new int[]{320, 360, 427, 475, 476, 487, 488, 640, 960, 1280}) {
            var toolbar = new LayoutToolbarGeometry(width);
            assertTrue(toolbar.x >= 0 && toolbar.x + toolbar.width <= width);
            for (int group = 0; group < 4; group++) {
                int x = toolbar.groupX(group), y = toolbar.labelY(group);
                int right = x + toolbar.groupWidth(group), bottom = toolbar.buttonY(group) + 18;
                assertTrue(toolbar.contains(x, y) && toolbar.contains(right - 1, bottom - 1));
                assertTrue(y >= toolbar.y + 25, "group labels cannot overlap header navigation");
                for (int other = 0; other < group; other++) {
                    assertFalse(x < toolbar.groupX(other) + toolbar.groupWidth(other)
                            && right > toolbar.groupX(other) && y < toolbar.buttonY(other) + 18
                            && bottom > toolbar.labelY(other), "toolbar groups cannot overlap");
                }
            }
            assertTrue(toolbar.canvasTop() <= 128, "small GUIs retain usable canvas below the padded toolbar");
        }
    }

    @Test void panelHitAreaExcludesTheCanvasAndOutsideMargins() {
        var toolbar = new LayoutToolbarGeometry(640);
        assertTrue(toolbar.contains(toolbar.x + 2, toolbar.y + 2));
        assertFalse(toolbar.contains(toolbar.x - 1, toolbar.y + 2));
        assertFalse(toolbar.contains(toolbar.x + 2, toolbar.canvasTop()));
        assertFalse(toolbar.contains(toolbar.x + toolbar.width, toolbar.y));
    }
}

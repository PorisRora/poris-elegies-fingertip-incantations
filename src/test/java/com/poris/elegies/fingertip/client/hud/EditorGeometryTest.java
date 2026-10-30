package com.poris.elegies.fingertip.client.hud;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EditorGeometryTest {
    @Test void sourceAndInspectorFitWithoutCoveringTheDefaultHudRow() {
        for (int[] size : new int[][]{{320,200}, {360,240}, {427,267}, {640,400}, {960,540}}) {
            for (int count : new int[]{0, 1, 9, 18, 64}) {
                var ui = new EditorGeometry(size[0], size[1], count);
                assertTrue(ui.x >= 0 && ui.y >= 0);
                assertTrue(ui.x + ui.width <= size[0] && ui.y + ui.height <= size[1]);
                assertTrue(ui.x >= 8 && ui.y >= 8, "outer margin is not flush with the window");
                assertTrue(ui.toolbarX(0) - ui.x >= 6, "toolbar has inner padding");
                assertTrue(ui.x + ui.width - (ui.toolbarX(3) + ui.toolbarWidth()) >= 6);
                assertTrue(ui.titleWidth() >= 120, "title retains room beside the outer controls");
                assertTrue(ui.enableX() + ui.enableWidth() < ui.modeX());
                assertTrue(ui.modeX() + ui.modeWidth() < ui.doneX());
                assertTrue(ui.y + ui.headerInset + ui.toolbarButtonHeight < ui.toolbarY);
                assertTrue(ui.y + ui.headerHeight - (ui.toolbarY + ui.toolbarButtonHeight) >= 6);
                assertTrue(ui.bodyTop - ui.panelPad >= ui.y + ui.headerHeight + 6, "panels have a separate vertical gap");
                assertTrue(ui.rightWidth >= ui.inventoryWidth, "source column holds all nine inventory columns");
                assertTrue(ui.inventoryY(0) + ui.inventoryCell <= ui.bodyBottom, "inventory stays above the footer");
                if (ui.compact) {
                    assertTrue(ui.bodyBottom + ui.panelPad <= size[1] - 8, "compact panel retains its outer margin");
                    assertTrue(ui.listY + 23 + 22 + 48 <= ui.bodyBottom, "first source card fits below its group header");
                } else assertTrue(ui.bodyBottom + ui.panelPad < size[1] - 22, "normal source panel leaves the HUD row visible");
            }
        }
    }

    @Test void inventoryHitAreasMatchAllThirtySixDisplayedCells() {
        for (int[] size : new int[][]{{320, 200}, {640, 400}}) {
            var ui = new EditorGeometry(size[0], size[1], 9);
            for (int i = 0; i < 36; i++) {
                assertEquals(i, ui.inventoryAt(ui.inventoryX(i), ui.inventoryY(i)));
                assertEquals(i, ui.inventoryAt(ui.inventoryX(i) + ui.inventoryCell - .1, ui.inventoryY(i) + ui.inventoryCell - .1));
            }
            assertEquals(-1, ui.inventoryAt(ui.inventoryX() - .01, ui.listY));
            assertEquals(-1, ui.inventoryAt(ui.inventoryX(), ui.inventoryY(0) - 1));
        }
    }

    @Test void addingSlotsDoesNotMoveTheLibraryOrInspector() {
        var single = new EditorGeometry(640, 400, 4);
        var multi = new EditorGeometry(640, 400, 64);
        assertEquals(single.rightX, multi.rightX);
        assertEquals(single.bodyBottom, multi.bodyBottom);
    }

    @Test void aShortButWideWindowShowsTheFullModeLabel() {
        var user = new EditorGeometry(512, 274, 9);   // 2560x1369 at GUI scale 5
        assertTrue(user.compact);
        assertTrue(user.modeWidth() >= 70, "the mode button has room for its full label");
        assertTrue(user.titleWidth() >= 120);
        assertEquals(60, new EditorGeometry(320, 200, 9).modeWidth(), "a narrow window still falls back to the short label");
    }
}

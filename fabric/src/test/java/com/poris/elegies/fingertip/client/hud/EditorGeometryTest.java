package com.poris.elegies.fingertip.client.hud;

import org.junit.jupiter.api.Test;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;

class EditorGeometryTest {
    /** Texts short enough for any size, so the size hangs on the room alone. */
    private static final Predicate<EditorGeometry> SHORT_TEXTS = g -> true;
    /**
     * Below Minecraft's smallest GUI, at it, what vanilla's auto GUI scale gives common screens (2560x1369: 512x274), and
     * the user's under Modern UI's (640x343, and 640x372 on the laptop).
     */
    private static final int[][] SIZES = {{320,200}, {320,240}, {360,240}, {412,258}, {427,240}, {427,267}, {456,256},
            {480,270}, {512,274}, {534,300}, {640,343}, {640,372}, {854,457}, {960,540}};

    @Test void sourceAndInspectorFitWithoutCoveringTheDefaultHudRow() {
        for (int[] size : SIZES) {
            var ui = EditorGeometry.of(size[0], size[1], SHORT_TEXTS);
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
            assertTrue(ui.bodyBottom + ui.panelPad <= size[1] - 8, "the panel retains its outer margin");
            assertTrue(ui.listY + 23 + 22 + 48 <= ui.bodyBottom, "first source card fits below its group header");
            if (size[1] >= 240) assertTrue(ui.hintHeight() >= 20, "the items page has two lines of hint from Minecraft's smallest GUI up");
        }
    }

    @Test void theNormalSizeWhereItFitsTheCompactOneElsewhere() {
        // 1.1.6 (2026-11-01): as through 1.1.4, the normal size wherever it fits.
        for (int[] size : new int[][]{{320, 240}, {427, 240}, {480, 270}, {512, 274}})
            assertTrue(EditorGeometry.of(size[0], size[1], SHORT_TEXTS).compact, size[0] + "x" + size[1]);
        for (int[] size : new int[][]{{534, 300}, {640, 343}, {640, 372}, {854, 457}}) {
            var ui = EditorGeometry.of(size[0], size[1], SHORT_TEXTS);
            assertFalse(ui.compact, size[0] + "x" + size[1]);
            assertEquals(22, ui.inventoryCell); assertEquals(60, ui.headerHeight);
        }
        assertTrue(EditorGeometry.of(640, 343, g -> false).compact, "texts the normal size cannot show whole fall back to compact");
        var user = EditorGeometry.of(640, 343, SHORT_TEXTS);
        assertTrue(user.bodyBottom - user.bodyTop > 137, "the skills and items pages get more room than in the compact panel");
    }

    @Test void inventoryHitAreasMatchAllThirtySixDisplayedCells() {
        for (int[] size : new int[][]{{320, 200}, {640, 400}}) {
            var ui = EditorGeometry.of(size[0], size[1], SHORT_TEXTS);
            for (int i = 0; i < 36; i++) {
                assertEquals(i, ui.inventoryAt(ui.inventoryX(i), ui.inventoryY(i)));
                assertEquals(i, ui.inventoryAt(ui.inventoryX(i) + ui.inventoryCell - .1, ui.inventoryY(i) + ui.inventoryCell - .1));
            }
            assertEquals(-1, ui.inventoryAt(ui.inventoryX() - .01, ui.listY));
            assertEquals(-1, ui.inventoryAt(ui.inventoryX(), ui.inventoryY(0) - 1));
        }
    }

    @Test void aShortButWideWindowShowsTheFullModeLabel() {
        var vanilla = EditorGeometry.of(512, 274, SHORT_TEXTS);   // 2560x1369 at vanilla's auto GUI scale 5: compact
        assertTrue(vanilla.compact);
        assertTrue(vanilla.modeWidth() >= 70, "the mode button has room for its full label");
        assertTrue(vanilla.titleWidth() >= 120);
        assertEquals(60, EditorGeometry.of(320, 200, SHORT_TEXTS).modeWidth(), "a narrow window still falls back to the short label");
    }
}

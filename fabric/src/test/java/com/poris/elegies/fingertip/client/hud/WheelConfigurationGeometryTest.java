package com.poris.elegies.fingertip.client.hud;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WheelConfigurationGeometryTest {
    @Test void wheelAndFooterRemainSeparateAtSupportedGuiScales() {
        for (int[] size : new int[][]{{320,200}, {360,240}, {427,267}, {512,273}, {640,400}, {960,540}}) {
            var ui = new WheelConfigurationGeometry(size[0], size[1]);
            var wheel = ui.wheel;
            assertTrue(wheel.cx() - wheel.outer() >= 16);
            assertTrue(wheel.cx() + wheel.outer() <= size[0] - 16);
            assertTrue(wheel.cy() - wheel.outer() >= 36);
            assertTrue(wheel.cy() + wheel.outer() + 10 <= ui.footerY);
            assertTrue(ui.footerY + 20 <= size[1] - 8);
            assertTrue(ui.toggleX >= 16 && ui.doneX + WheelConfigurationGeometry.DONE_WIDTH <= size[0] - 16);
            assertTrue(ui.toggleX + ui.toggleWidth + WheelConfigurationGeometry.GAP <= ui.doneX, "switch and Done must not touch");
            assertEquals(16, ui.modeX, "the drag/click switch sits at the left end of the footer");
            assertTrue(ui.modeX + WheelConfigurationGeometry.MODE_WIDTH + WheelConfigurationGeometry.GAP <= ui.toggleX, "the two switches must not touch");
            assertTrue(ui.toggleWidth >= 96, "the wheel switch keeps room for its label at " + size[0]);
            for (int sector=0; sector<RadialGeometry.COUNT; sector++)
                assertEquals(sector, wheel.atPreview(wheel.labelX(sector), wheel.labelY(sector)));
        }
    }
    @Test void switchIsCentredUnderTheWheelAndDoneSitsAtTheRightEdge() {
        for (int[] size : new int[][]{{320,200}, {427,267}, {512,273}, {960,540}}) {
            var ui = new WheelConfigurationGeometry(size[0], size[1]);
            assertEquals(ui.wheel.cx() * 2, ui.toggleX * 2 + ui.toggleWidth, "switch centred under the wheel at " + size[0]);
            assertEquals(size[0] - 16, ui.doneX + WheelConfigurationGeometry.DONE_WIDTH);
        }
    }
}

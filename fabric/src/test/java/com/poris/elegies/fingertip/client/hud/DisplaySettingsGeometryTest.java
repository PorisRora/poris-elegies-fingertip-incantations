package com.poris.elegies.fingertip.client.hud;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DisplaySettingsGeometryTest {
    @Test void floatingControlsFitTheSmallestSupportedGui() {
        for (int w : new int[]{320,360,427,440,480,640,960,1280}) for (int h : new int[]{200,220,260,300,360,720}) {
            var occupied = List.of(new HudPlacement.Rect(w / 2 - 106, h - 24, 212, 20));
            var ui = new DisplaySettingsGeometry(w,h,occupied);
            var screen = new HudPlacement.Rect(0,0,w,h);
            assertTrue(screen.contains(ui.panel));
            assertTrue(screen.contains(ui.restore));
            assertTrue(ui.panel.width() <= 232 && ui.panel.height() <= 192);
            assertTrue(ui.panel.contains(ui.done));
            assertTrue(ui.panel.contains(ui.functions()));
            assertTrue(ui.panel.contains(ui.durability()));
            assertEquals(0, ui.durability().overlap(ui.functions()));
            assertEquals(0, ui.durability().overlap(ui.done));
            for (int lines = 1; lines <= 2; lines++) {
                var hint = ui.hint(lines);
                assertTrue(ui.panel.contains(hint));
                assertEquals(0, hint.overlap(ui.done));
                assertEquals(ui.done.y() * 2 + ui.done.height(), hint.y() * 2 + hint.height(), "the Tab hint is centred on Done");
            }
            for(int i=0;i<3;i++) {
                assertTrue(ui.panel.contains(ui.theme(i)) && ui.panel.contains(ui.range(i)));
                assertTrue(ui.theme(i).width() >= 60, "theme names need full readable text");
                assertEquals(0,ui.theme(i).overlap(ui.range(i)));
                for(int j=i+1;j<3;j++) {
                    assertEquals(0,ui.theme(i).overlap(ui.theme(j)));
                    assertEquals(0,ui.range(i).overlap(ui.range(j)));
                }
            }
        }
    }

    @Test void panelAndRestoreButtonAvoidOccupiedCornersIndependently() {
        var topRight = new HudPlacement.Rect(400,0,240,190);
        var ui = new DisplaySettingsGeometry(640,360,List.of(topRight));
        assertEquals(0,ui.panel.overlap(topRight));
        assertEquals(0,ui.restore.overlap(topRight));
        assertEquals(8,ui.panel.x());
        assertEquals(8,ui.restore.x());
    }

    @Test void emptyCanvasDefaultsToTopRightWithoutAFullScreenPanel() {
        var ui = new DisplaySettingsGeometry(640,360,List.of());
        assertEquals(400,ui.panel.x());
        assertEquals(8,ui.panel.y());
        assertEquals(556,ui.restore.x());
    }
}

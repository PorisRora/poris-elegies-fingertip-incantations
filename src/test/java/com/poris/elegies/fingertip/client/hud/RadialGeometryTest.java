package com.poris.elegies.fingertip.client.hud;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RadialGeometryTest {
    @Test void labelsAndSelectionAgreeAtEverySupportedScale() {
        for (int[] size : new int[][]{{320,200},{427,267},{640,400},{1920,1080}}) {
            var g=RadialGeometry.fit(size[0],size[1]);
            for (int i=0;i<10;i++) assertEquals(i,g.at(g.labelX(i),g.labelY(i)));
            assertEquals(-1,g.at(g.cx(),g.cy()));
            assertEquals(-1,g.at(g.cx(),g.cy()-g.inner()+.01));
            assertEquals(-1,g.at(g.cx()+g.outer()+21,g.cy()));
            assertTrue(g.cy()-g.outer()>=0 && g.cy()+g.outer()<size[1]);
        }
    }
    @Test void settingsPreviewDoesNotClaimTheAdjacentPickerAndHasTheSameSectorOrder() {
        for (int[] size : new int[][]{{308,192}, {415,259}, {560,330}}) {
            var ui=WheelSettingsGeometry.fit(6,55,size[0],4+size[1]-30); var g=ui.wheel();
            assertTrue(g.cx()+g.outer()+2 < ui.pickerX());
            assertTrue(g.cy()-g.outer()>=55);
            assertTrue(ui.pickerY()+4*ui.rowStep()+ui.buttonHeight()<=4+size[1]-30);
            assertEquals(-1,g.atPreview(g.cx()+g.outer()+1,g.cy()));
            for (int i=0;i<10;i++) assertEquals(i,g.atPreview(g.labelX(i),g.labelY(i)));
        }
    }
    @Test void sectorBoundariesAreClockwiseWithoutDeadGaps() {
        var g=RadialGeometry.fit(640,400);
        for(int i=0;i<10;i++) {
            double angle=i*RadialGeometry.STEP+RadialGeometry.STEP/2;
            assertEquals(i,g.at(g.cx()+80*Math.sin(angle-.00001),g.cy()-80*Math.cos(angle-.00001)));
            assertEquals((i+1)%10,g.at(g.cx()+80*Math.sin(angle+.00001),g.cy()-80*Math.cos(angle+.00001)));
        }
    }
}

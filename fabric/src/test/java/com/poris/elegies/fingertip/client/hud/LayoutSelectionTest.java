package com.poris.elegies.fingertip.client.hud;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class LayoutSelectionTest {
    private final List<HudLayout.Slot> slots = List.of(new HudLayout.Slot(0, -100, -60),
            new HudLayout.Slot(1, -60, -60), new HudLayout.Slot(2, 0, -100));
    @Test void boxWorksInBothDirectionsWithoutMovingAnything() {
        for (boolean reverse : new boolean[]{false, true}) {
            var selection = new LayoutSelection();
            selection.begin(reverse ? 128 : 55, reverse ? 165 : 135, false);
            selection.move(reverse ? 55 : 128, reverse ? 135 : 165, slots, 320, 200);
            assertEquals(Set.of(0, 1), selection.ids); selection.end();
            assertEquals(-100, slots.getFirst().dx);   // the box selected it without moving it
        }
    }
    @Test void addSelectionAndCancelPreservePriorSelection() {
        var selection = new LayoutSelection(); selection.single(2);
        selection.begin(55, 135, true); selection.move(85, 165, slots, 320, 200);
        assertEquals(Set.of(0, 2), selection.ids); selection.end();
        selection.begin(95, 135, false); selection.move(125, 165, slots, 320, 200);
        assertEquals(Set.of(1), selection.ids); selection.cancel();
        assertEquals(Set.of(0, 2), selection.ids);
    }
    @Test void emptyClickClearsSelectionAndToggleOnlyChangesMembership() {
        var selection = new LayoutSelection(); selection.all(slots);
        selection.begin(10, 10, false); selection.move(10, 10, slots, 320, 200); selection.end();
        assertTrue(selection.ids.isEmpty()); selection.toggle(1); selection.toggle(2); selection.toggle(1);
        assertEquals(Set.of(2), selection.ids);
    }
    @Test void nativeRowSelectionIsAtomicButExtrasRemainIndependent() {
        var selection = new LayoutSelection(); selection.linked(Set.of(0, 1));
        selection.single(1); assertEquals(Set.of(0, 1), selection.ids);
        selection.toggle(0); assertTrue(selection.ids.isEmpty());
        selection.begin(55, 135, false); selection.move(85, 165, slots, 320, 200); selection.end();
        assertEquals(Set.of(0, 1), selection.ids);
        selection.toggle(2); assertEquals(Set.of(0, 1, 2), selection.ids);
        selection.toggle(1); assertEquals(Set.of(2), selection.ids);
        selection.linked(Set.of()); selection.single(0); assertEquals(Set.of(0), selection.ids);
    }
}

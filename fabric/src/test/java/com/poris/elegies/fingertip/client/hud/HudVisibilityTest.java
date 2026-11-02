package com.poris.elegies.fingertip.client.hud;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudVisibilityTest {
    @Test void independentFiltersCombineWithoutHidingItemsOrSkills() {
        for (var mode : HudVisibility.Mode.values()) for (boolean hide : new boolean[]{false, true}) {
            assertEquals(mode != HudVisibility.Mode.HIDDEN,
                    HudVisibility.includes(mode, hide, false, false, false), "bound item or skill remains visible unless all hidden");
            assertEquals(mode == HudVisibility.Mode.ALL,
                    HudVisibility.includes(mode, hide, false, true, false), "unbound frame follows only empty/all filter");
            assertEquals(mode != HudVisibility.Mode.HIDDEN && !hide,
                    HudVisibility.includes(mode, hide, false, false, true), "function follows independent function filter");
            assertEquals(mode != HudVisibility.Mode.HIDDEN,
                    HudVisibility.includes(mode, hide, true, true, false), "native row stays intact except hide all");
        }
    }
    @Test void invalidModesFallBackAndCycleHasExactlyThreeStates() {
        assertEquals(HudVisibility.Mode.ALL, HudVisibility.Mode.from(null));
        assertEquals(HudVisibility.Mode.ALL, HudVisibility.Mode.from("malformed"));
        assertEquals(HudVisibility.Mode.NONEMPTY, HudVisibility.Mode.ALL.next());
        assertEquals(HudVisibility.Mode.HIDDEN, HudVisibility.Mode.NONEMPTY.next());
        assertEquals(HudVisibility.Mode.ALL, HudVisibility.Mode.HIDDEN.next());
    }
}

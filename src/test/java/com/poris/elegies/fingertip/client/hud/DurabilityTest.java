package com.poris.elegies.fingertip.client.hud;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DurabilityTest {
    @Test void roundsDownButKeepsTheLastUseVisible() {
        assertEquals(100, Durability.percent(0, 250));
        assertEquals(50, Durability.percent(125, 250));
        assertEquals(99, Durability.percent(1, 1561), "one point of wear is no longer 100%");
        assertEquals(1, Durability.percent(1560, 1561), "one use left never reads 0%");
        assertEquals(0, Durability.percent(250, 250));
    }

    @Test void largeDurabilityDoesNotOverflow() {
        assertEquals(50, Durability.percent(Integer.MAX_VALUE / 2, Integer.MAX_VALUE - 1));
    }
}

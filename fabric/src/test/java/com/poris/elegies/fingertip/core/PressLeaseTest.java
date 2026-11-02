package com.poris.elegies.fingertip.core;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class PressLeaseTest {
    @Test void rebindingReleasesTheOldActionAndOnlyOnce() {
        var events = new ArrayList<String>(); var lease = new PressLease();
        lease.begin(() -> events.add("old press"), () -> events.add("old release"));
        lease.begin(() -> events.add("new press"), () -> events.add("new release"));
        lease.end(); lease.end();
        assertEquals(java.util.List.of("old press", "old release", "new press", "new release"), events);
    }
    @Test void failedProviderDoesNotLeaveItsKeyHeld() {
        var events = new ArrayList<String>(); var lease = new PressLease();
        assertThrows(IllegalStateException.class, () -> lease.begin(() -> { throw new IllegalStateException(); }, () -> events.add("release")));
        assertFalse(lease.active()); assertEquals(1, events.size());
    }
}

package com.poris.elegies.fingertip.core;

import com.poris.elegies.fingertip.client.input.WheelActionRef;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WheelActionRefTest {
    @Test void preservesCompleteMappingNamesAndApiNamespaces() {
        String key = "key.yes_steve_model.animation_wheel";
        assertEquals("key:" + key, WheelActionRef.fromCatalogId("mapping:" + key));
        var mapping = WheelActionRef.parse(WheelActionRef.key(key)).orElseThrow();
        assertEquals(WheelActionRef.Kind.KEY, mapping.kind()); assertEquals(key, mapping.target());
        String api = "optional_mod:abilities/open-wheel";
        assertEquals("action:" + api, WheelActionRef.fromCatalogId(api));
        assertEquals(api, WheelActionRef.parse(WheelActionRef.action(api)).orElseThrow().target());
    }
    @Test void nativeIdsRemainBackwardCompatibleAndMalformedEntriesAreRejected() {
        for (String id : WheelActionRef.NATIVE_IDS) {
            var parsed = WheelActionRef.parse(id).orElseThrow();
            assertEquals(WheelActionRef.Kind.NATIVE, parsed.kind()); assertEquals(id, parsed.target());
        }
        for (String bad : new String[]{"unknown", "key:", "key:   ", "key:broken\nkey", "action:", "action:Upper:Bad", "action:no_namespace", "action:mod:"})
            assertTrue(WheelActionRef.parse(bad).isEmpty(), bad);
        assertTrue(WheelActionRef.parse(null).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> WheelActionRef.fromCatalogId("mapping:"));
        assertThrows(IllegalArgumentException.class, () -> WheelActionRef.action("bad"));
    }
    @Test void distinctRegistriesCannotCollideWithBuiltInActions() {
        assertNotEquals(WheelActionRef.parse("inventory"), WheelActionRef.parse("key:inventory"));
        assertNotEquals(WheelActionRef.parse("key:mod:feature"), WheelActionRef.parse("action:mod:feature"));
    }
}

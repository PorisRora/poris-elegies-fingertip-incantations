package com.poris.elegies.fingertip.core;

import com.poris.elegies.fingertip.client.input.InputPreferences;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class InputPreferencesTest {
    @Test void defaultsDoNotTakeAnyOptionalNativeKey() {
        var p=new InputPreferences();
        for(int key=0;key<350;key++) assertFalse(p.claims(key));
        assertTrue(p.wheelEnabled, "the wheel is on for a new install (0.1-beta)");
        assertEquals(10,p.wheel.size()); assertEquals("editor",p.wheel.get(5)); assertEquals("drop_stack",p.wheel.get(7));
    }
    @Test void groupedFunctionKeysAndIndividualExemptionsAreIndependentOfQet() {
        var p=new InputPreferences(); p.functionKeys=true; p.functionMask&=~(1<<2); p.takeQ=true;
        for(int key=290;key<=298;key++) assertEquals(key!=292,p.claims(key));
        assertTrue(p.claims(81)); assertFalse(p.claims(69)); assertFalse(p.claims(84)); assertFalse(p.claims(299));
        p.functionKeys=false; p.takeE=true;
        assertFalse(p.claims(290)); assertTrue(p.claims(69)); assertTrue(p.claims(81));
    }
    @Test void malformedWheelKeepsValidChoicesAndAlwaysHasAnEditorExit() {
        var p=new InputPreferences(); p.wheel=Arrays.asList("drop",null,"unknown","chat","none","hud","swap","inventory","extra");
        p.normalize(); assertEquals(10,p.wheel.size()); assertEquals("inventory",p.wheel.getFirst());
        assertEquals("chat",p.wheel.get(1)); assertEquals("screenshot",p.wheel.get(2)); assertEquals("editor",p.wheel.get(5));
        p.wheel=null;p.normalize();assertEquals(InputPreferences.defaults(),p.wheel);
    }
    @Test void takingEAlwaysKeepsTheWheelAndAnInventoryExitAvailable() {
        var p = new InputPreferences();
        p.takeE = true; p.wheelEnabled = false; p.wheel.set(0, "drop"); p.normalize();
        assertTrue(p.wheelEnabled); assertEquals("inventory", p.wheel.getFirst());
        assertTrue(p.lockedWheelSlot(0)); assertTrue(p.lockedWheelSlot(5));
        p.wheelEnabled = false; p.normalize(); assertTrue(p.wheelEnabled, "a stale saved flag cannot remove the exit");
        p.takeE = false; p.normalize();
        assertTrue(p.wheelEnabled, "releasing E does not unexpectedly close a wheel the player uses");
        assertTrue(p.lockedWheelSlot(0), "top remains the inventory anchor even when E is native");
        p.wheelEnabled = false; p.normalize(); assertFalse(p.wheelEnabled);
    }
    @Test void fixedTopSwapsWithTheOldInventorySectorInsteadOfDiscardingItsCustomAction() {
        var p = new InputPreferences(); p.wheelEnabled = false; p.wheel.set(0, "drop"); p.wheel.set(7, "inventory");
        p.normalize();
        assertEquals("inventory", p.wheel.getFirst()); assertEquals("drop", p.wheel.get(7));
        assertFalse(p.wheelEnabled, "pinning is not an instruction to enable the optional wheel");
        for (int i = 0; i < 10; i++) assertEquals(i == 0 || i == 5, p.lockedWheelSlot(i));
    }
    @Test void displacedUniqueActionsSurviveWhenAnchorsWereCustomizedWithoutAnEmptySlot() {
        var p = new InputPreferences();
        p.wheel = new java.util.ArrayList<>(java.util.List.of("drop", "chat", "swap", "perspective", "hud", "screenshot", "drop", "pick", "swap", "hud"));
        var actions = new java.util.HashSet<>(p.wheel);
        p.normalize();
        assertEquals("inventory", p.wheel.get(0)); assertEquals("editor", p.wheel.get(5));
        assertTrue(p.wheel.containsAll(actions));
        var normalized = java.util.List.copyOf(p.wheel); p.normalize(); assertEquals(normalized, p.wheel);
    }
    @Test void swappedTopAndBottomAreRestoredWithoutChangingOtherSectors() {
        var p = new InputPreferences(); p.wheel.set(0, "editor"); p.wheel.set(5, "inventory");
        p.normalize(); assertEquals(InputPreferences.defaults(), p.wheel);
    }
    @Test void optionalModActionsSurviveNormalizationWithoutNeedingAnInstalledRegistry() {
        var p = new InputPreferences();
        p.wheel.set(2, "key:key.removed_mod.open_menu");
        p.wheel.set(7, "action:removed_mod:ability/open");
        p.normalize();
        assertEquals("key:key.removed_mod.open_menu", p.wheel.get(2));
        assertEquals("action:removed_mod:ability/open", p.wheel.get(7));
        assertEquals("inventory", p.wheel.get(0)); assertEquals("editor", p.wheel.get(5));
        var saved = java.util.List.copyOf(p.wheel); p.normalize(); assertEquals(saved, p.wheel);
    }
    @Test void newlyPinnedTopRelocatesADynamicActionIntoItsExistingInventoryPosition() {
        var p = new InputPreferences();
        p.wheel.set(0, "key:key.some_mod.panel"); p.wheel.set(3, "inventory");
        p.normalize();
        assertEquals("inventory", p.wheel.get(0)); assertEquals("key:key.some_mod.panel", p.wheel.get(3));
    }
}

package com.poris.elegies.fingertip.client.input;

import com.poris.elegies.fingertip.client.input.KeyState;
import com.mojang.blaze3d.platform.InputConstants;
import com.poris.elegies.fingertip.mixin.client.KeyMappingAccess;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.lwjgl.glfw.GLFW;
import java.util.Set;

/** One owner for gameplay input. GUI input and the player's original key settings stay intact. */
public final class NativeInputPolicy {
    private static Options options;
    private static Set<KeyMapping> basics = Set.of();
    private static final ThreadLocal<Boolean> PASSTHROUGH = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> PHYSICAL_WORLD = ThreadLocal.withInitial(() -> false);

    public static boolean active() {
        var mc = Minecraft.getInstance();
        return HotbarControl.managesGameplay() && mc.isWindowActive() && mc.screen == null && mc.getOverlay() == null;
    }

    /** Identity matters: a mod using a vanilla category/name does not become a basic control. */
    public static Set<KeyMapping> basics() {
        var o = Minecraft.getInstance().options;
        if (o != options) {
            options = o;
            basics = Set.of(o.keyUp, o.keyDown, o.keyLeft, o.keyRight, o.keyJump, o.keyShift, o.keySprint,
                    o.keyAttack, o.keyUse, o.keyInventory, o.keyDrop, o.keySwapOffhand, o.keyChat,
                    o.keyCommand, o.keyPlayerList, o.keySocialInteractions, o.keyScreenshot,
                    o.keyTogglePerspective, o.keySmoothCamera, o.keyFullscreen, o.keySpectatorOutlines,
                    o.keyAdvancements, ShortcutKeys.OPEN);
        }
        return basics;
    }

    public static boolean managed(KeyMapping mapping) {
        if (ShortcutKeys.index(mapping) >= 0 || mapping == ShortcutKeys.OPEN) return false;
        if (OptionalKeyPolicy.wheelKey(KeyState.key(mapping))) return true;
        if (OptionalKeyPolicy.passthrough(KeyState.key(mapping))) return false;
        if (OptionalKeyPolicy.claimed(KeyState.key(mapping)) && !essential(mapping)) return true;
        return !basics().contains(mapping) && !HotbarControl.preserves(mapping);
    }
    public static boolean blocks(KeyMapping mapping) { return active() && managed(mapping); }
    /** A press reaching a mapping Fingertip owns, other than one Fingertip itself relays to it: setDown must skip it. */
    public static boolean refusesDown(KeyMapping mapping, boolean down) {
        return down && blocks(mapping) && !NativeKeyRelay.writing(mapping);
    }
    public static boolean essential(KeyMapping mapping) {
        var o = Minecraft.getInstance().options;
        return mapping == o.keyUp || mapping == o.keyDown || mapping == o.keyLeft || mapping == o.keyRight
                || mapping == o.keyJump || mapping == o.keyShift || mapping == o.keySprint
                || mapping == o.keyAttack || mapping == o.keyUse || mapping == ShortcutKeys.OPEN;
    }
    /** Static reservation for saved bindings; optional keys are allowed to stay saved while paused. */
    public static boolean reservedBinding(InputConstants.Key key) {
        return !key.equals(InputConstants.UNKNOWN) && basics().stream().anyMatch(k -> KeyState.key(k).equals(key)
                && (essential(k) || !OptionalKeyPolicy.optional(key)))
                || key.getType() == InputConstants.Type.KEYSYM && key.getValue() == GLFW.GLFW_KEY_ESCAPE;
    }
    public static boolean basicKey(InputConstants.Key key) {
        return !key.equals(InputConstants.UNKNOWN) && basics().stream().anyMatch(k -> KeyState.key(k).equals(key) && (!OptionalKeyPolicy.claimed(key) || essential(k)));
    }
    public static boolean systemKey(InputConstants.Key key) {
        if (key.getType() != InputConstants.Type.KEYSYM) return false;
        int code = key.getValue();
        return code == GLFW.GLFW_KEY_ESCAPE || OptionalKeyPolicy.passthrough(key);
    }
    public static boolean nativeKey(InputConstants.Key key) {
        return !OptionalKeyPolicy.wheelKey(key) && (basicKey(key) || systemKey(key) || HotbarControl.preservesKey(key));
    }

    /** Filter before NeoForge's modifier-priority lookup, so a mod's Alt+W cannot steal movement. */
    public static boolean nativeState(InputConstants.Key key, boolean down, boolean click) {
        if (!active()) return false;
        if (OptionalKeyPolicy.passthrough(key)) {
            // Let vanilla and external mappings use a released optional key, but never queue
            // clicks on the paused HUD mapping which happens to share that physical key.
            for (var mapping : KeyState.boundTo(key)) {
                if (ShortcutKeys.index(mapping) >= 0 || key.equals(InputConstants.UNKNOWN)) continue;
                if (down && !KeyState.matches(mapping)) continue;
                if (click) InputRouter.click(mapping); else mapping.setDown(down);
            }
            return true;
        }
        for (var mapping : KeyMappingAccess.fingertip$all().values()) {
            if (managed(mapping) || !basics().contains(mapping) && !HotbarControl.preserves(mapping)) continue;
            if (!KeyState.key(mapping).equals(key) || key.equals(InputConstants.UNKNOWN)) continue;
            if (down && !KeyState.matches(mapping)) continue;
            if (click) InputRouter.click(mapping); else mapping.setDown(down);
        }
        return true;
    }

    /** Remember where a callback began even if vanilla opens a screen during that callback. */
    public static boolean beginPhysical() {
        boolean previous = PHYSICAL_WORLD.get();
        PHYSICAL_WORLD.set(previous || active());
        return previous;
    }
    public static void endPhysical(boolean previous) {
        if (previous) PHYSICAL_WORLD.set(true); else PHYSICAL_WORLD.remove();
    }
    public static void retainWorldRelease() { PHYSICAL_WORLD.set(true); }
    public static boolean beginPassthrough(InputConstants.Key key) {
        boolean previous = PASSTHROUGH.get(); PASSTHROUGH.set(previous || OptionalKeyPolicy.passthrough(key)); return previous;
    }
    public static void endPassthrough(boolean previous) { if (previous) PASSTHROUGH.set(true); else PASSTHROUGH.remove(); }
    public static boolean suppressRaw() { return !PASSTHROUGH.get() && (PHYSICAL_WORLD.get() || active()); }

    public static void clearUnownedState() {
        if (!active()) return;
        for (var mapping : KeyMappingAccess.fingertip$all().values()) {
            if (!managed(mapping)) continue;
            var access = (KeyMappingAccess)mapping;
            if (!NativeKeyRelay.isHeld(mapping) && !NativeKeyRelay.ownsToggle(mapping)) access.fingertip$down(false);
            if (!NativeKeyRelay.hasClicks(mapping)) access.fingertip$clickCount(0);
        }
    }
    private NativeInputPolicy() {}
}

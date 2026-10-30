package com.poris.elegies.fingertip.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import com.poris.elegies.fingertip.core.PressLease;
import org.lwjgl.glfw.GLFW;
import java.util.HashMap;
import java.util.Map;

/** Keeps both routed and suppressed presses paired with their own release across GUI changes. */
public final class PhysicalInputRouter {
    private record Press(KeyMapping owner, boolean nativeControl, java.util.List<PressLease> nativeModLeases) {
        Press(KeyMapping owner, boolean nativeControl) { this(owner, nativeControl, java.util.List.of()); }
        void releaseMods() {
            for (var lease : nativeModLeases) try { lease.end(); }
            catch (RuntimeException error) { com.poris.elegies.fingertip.api.FingertipApi.report("native-mod-release", error); }
        }
    }
    private static final Map<InputConstants.Key, Press> PRESSES = new HashMap<>();

    public static boolean route(InputConstants.Key key, int action) {
        if (action == GLFW.GLFW_RELEASE) {
            Press press = PRESSES.remove(key);
            if (press != null) {
                press.releaseMods();
                if (press.nativeControl()) {
                    // Still clear vanilla movement/use after a screen opens, without leaking a raw release.
                    NativeInputPolicy.retainWorldRelease();
                    return false;
                }
                if (press.owner() != null) press.owner().setDown(false);
                return true;
            }
            return NativeInputPolicy.active() && !NativeInputPolicy.nativeKey(key);
        }
        if (action == GLFW.GLFW_REPEAT && PRESSES.containsKey(key))
            return !PRESSES.get(key).nativeControl() || !NativeInputPolicy.active();
        var old = PRESSES.remove(key);
        if (old != null) old.releaseMods();
        // While F3 is held the next key is a vanilla debug combination (G chunk borders, B hitboxes, ...), as the
        // design promises. Vanilla checks it later in the same keyPress, so a claimed key never got there (2026-10-28).
        if (key.getType() == InputConstants.Type.KEYSYM && key.getValue() != GLFW.GLFW_KEY_F3
                && InputConstants.isKeyDown(net.minecraft.client.Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_F3)) {
            PRESSES.put(key, new Press(null, true));
            return false;
        }
        var owner = InputRouter.claim(key);
        if (owner != null) {
            PRESSES.put(key, new Press(owner, false));
            owner.setDown(true);
            if (action == GLFW.GLFW_PRESS) InputRouter.click(owner);
            return true;
        }
        if (NativeInputPolicy.active() && OptionalKeyPolicy.passthrough(key)) {
            PRESSES.put(key, new Press(null, true)); return false;
        }
        if (NativeInputPolicy.active() && !OptionalKeyPolicy.claimed(key)) {
            var nativeMods = ModKeyWhitelist.matching(key);
            if (!nativeMods.isEmpty()) {
                var leases = new java.util.ArrayList<PressLease>();
                var press = new Press(null, false, leases);
                PRESSES.put(key, press); // Retain ownership even if a listener opens a screen during press.
                try {
                    for (var mapping : nativeMods) {
                        var lease = new PressLease(); leases.add(lease);
                        NativeKeyRelay.press(mapping, lease);
                    }
                } catch (RuntimeException error) {
                    press.releaseMods(); com.poris.elegies.fingertip.api.FingertipApi.report("native-mod-input", error);
                }
                return true;
            }
        }
        if (NativeInputPolicy.active() && !NativeInputPolicy.nativeKey(key)) {
            PRESSES.put(key, new Press(null, false));
            return true;
        }
        if (NativeInputPolicy.active() && action == GLFW.GLFW_PRESS) PRESSES.put(key, new Press(null, true));
        return false;
    }
    /** Keep the press tombstone until physical release; opening a GUI must not leak an unmatched release. */
    public static void releaseWhitelisted() { PRESSES.values().forEach(Press::releaseMods); }
    private PhysicalInputRouter() {}
}

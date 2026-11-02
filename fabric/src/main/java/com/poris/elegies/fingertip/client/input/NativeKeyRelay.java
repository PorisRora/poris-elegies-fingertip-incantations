package com.poris.elegies.fingertip.client.input;

import com.poris.elegies.fingertip.client.input.KeyState;
import com.mojang.blaze3d.platform.InputConstants;
import com.poris.elegies.fingertip.core.PressLease;
import com.poris.elegies.fingertip.mixin.client.KeyMappingAccess;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ToggleKeyMapping;
import org.lwjgl.glfw.GLFW;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.ArrayDeque;

/** Several HUD slots may hold one vanilla action. Releasing one must not release the others. */
public final class NativeKeyRelay {
    private static final class Held {
        int count;
        final RawKeyRelay.Stroke stroke;
        Held(KeyMapping target) { stroke = RawKeyRelay.stroke(target); }
    }
    private static final Map<KeyMapping, Held> HELD = new IdentityHashMap<>();
    private static final class ClickTicket { boolean pending; }
    private static final Map<KeyMapping, ArrayDeque<ClickTicket>> CLICKS = new IdentityHashMap<>();
    private static final Set<KeyMapping> TOGGLES = Collections.newSetFromMap(new IdentityHashMap<>());
    public static boolean isHeld(KeyMapping target) { return !(target instanceof ToggleKeyMapping) && HELD.containsKey(target); }
    public static boolean ownsToggle(KeyMapping target) { return TOGGLES.contains(target); }
    public static boolean writing(KeyMapping target) { return RawKeyRelay.targets(target) || HELD.containsKey(target); }
    public static boolean hasClicks(KeyMapping target) { return CLICKS.containsKey(target); }
    public static boolean consumeClick(KeyMapping target) {
        var tickets = CLICKS.get(target);
        if (tickets == null) return false;
        tickets.removeFirst().pending = false;
        if (tickets.isEmpty()) CLICKS.remove(target);
        return true;
    }
    public static void press(KeyMapping target, PressLease lease) {
        press(target, lease, false);
    }
    /** A transient wheel activation retracts only its own unconsumed synthetic click on release. */
    public static void pressOnce(KeyMapping target, PressLease lease) {
        press(target, lease, true);
    }
    private static void press(KeyMapping target, PressLease lease, boolean transientClick) {
        var ticket = new ClickTicket();
        boolean toggleWasOwned = TOGGLES.contains(target);
        try { lease.begin(() -> {
            Held held = HELD.computeIfAbsent(target, Held::new);
            held.count++;
            held.stroke.run(() -> {
                if (target instanceof ToggleKeyMapping) TOGGLES.add(target);
                target.setDown(true);
                ticket.pending = true;
                CLICKS.computeIfAbsent(target, ignored -> new ArrayDeque<>()).addLast(ticket);
                InputRouter.click(target);
            });
        }, () -> {
            try { release(target); }
            finally { if (transientClick) discardClick(target, ticket); }
        }); } catch (RuntimeException error) {
            discardClick(target, ticket);
            if (!toggleWasOwned) TOGGLES.remove(target);
            throw error;
        }
    }
    private static void discardClick(KeyMapping target, ClickTicket ticket) {
        if (!ticket.pending) return;
        ticket.pending = false;
        var tickets = CLICKS.get(target);
        if (tickets == null || !tickets.remove(ticket)) return;
        if (tickets.isEmpty()) CLICKS.remove(target);
        var access = (KeyMappingAccess)target;
        // Never restore a stale snapshot or clear another source's count: retract at most this ticket.
        access.fingertip$clickCount(Math.max(0, access.fingertip$clickCount() - 1));
    }
    private static void release(KeyMapping target) {
        Held held = HELD.get(target);
        if (held == null || --held.count > 0) return;
        HELD.remove(target);
        // Toggle bindings toggle only on a new press, never during held-state refresh/release.
        target.setDown(!(target instanceof ToggleKeyMapping) && physicallyDown(target));
    }
    public static void refreshHeld() {
        HELD.keySet().forEach(target -> { if (!(target instanceof ToggleKeyMapping)) target.setDown(true); });
    }
    public static void discardInactive() {
        if (NativeInputPolicy.active()) return;
        // An explicit wheel lease also works with no HUD item slots. Its own lifetime handles
        // cleanup; ordinary ownership maintenance must not erase it before a mod's next tick.
        var entries = CLICKS.entrySet().iterator();
        while (entries.hasNext()) {
            var entry = entries.next();
            if (HELD.containsKey(entry.getKey())) continue;
            ((KeyMappingAccess)entry.getKey()).fingertip$clickCount(0);
            entry.getValue().forEach(ticket -> ticket.pending = false);
            entries.remove();
        }
        for (var mapping : TOGGLES) ((KeyMappingAccess)mapping).fingertip$down(false);
        TOGGLES.clear();
    }
    private static boolean physicallyDown(KeyMapping target) {
        var key = KeyState.key(target);
        var mc = Minecraft.getInstance();
        if (NativeInputPolicy.blocks(target)) return false;
        if (!mc.isWindowActive() || mc.screen != null || key.equals(InputConstants.UNKNOWN) || !KeyState.active(target)
                || !KeyState.modifier(target).active()) return false;
        var owner = InputRouter.claim(key);
        if (owner != null && owner != target) return false;
        long window = mc.getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE) return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        return key.getType() == InputConstants.Type.KEYSYM && InputConstants.isKeyDown(window, key.getValue());
    }
    private NativeKeyRelay() {}
}

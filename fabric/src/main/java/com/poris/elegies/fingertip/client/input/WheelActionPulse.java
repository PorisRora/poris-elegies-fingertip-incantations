package com.poris.elegies.fingertip.client.input;

import com.poris.elegies.fingertip.Fingertip;
import com.poris.elegies.fingertip.api.FingertipApi;
import com.poris.elegies.fingertip.binding.HudBinding;
import com.poris.elegies.fingertip.core.PressLease;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

/** One deliberate wheel activation, held across a complete game tick without occupying a HUD slot. */
public final class WheelActionPulse {
    private record Activation(WheelActionRef action, PressLease lease, LocalPlayer player, ClientLevel level, long started) {}
    private static WheelActionRef pending;
    private static Activation current;
    private static long tick;
    private static boolean dispatching, cancelAfterDispatch;

    public static boolean enqueue(WheelActionRef action) {
        if (action.kind() == WheelActionRef.Kind.NATIVE || !gameplay()) return false;
        cancel();
        pending = action;
        return true;
    }
    public static boolean active() { return pending != null || current != null; }
    private static boolean gameplay() {
        var mc = Minecraft.getInstance();
        return InputRouter.enabled() && OptionalKeyPolicy.settings().wheelEnabled && mc.isWindowActive()
                && mc.player != null && mc.player.isAlive() && !mc.player.isSpectator()
                && mc.level != null && mc.getConnection() != null && mc.screen == null && mc.getOverlay() == null;
    }
    private static boolean sameWorld() {
        var mc = Minecraft.getInstance();
        return current == null || current.player() == mc.player && current.level() == mc.level;
    }
    public static void beforeTick() {
        tick++;
        if (!gameplay() || !sameWorld()) cancel();
    }
    public static void start() {
        if (pending == null) return;
        if (!gameplay()) { cancel(); return; }
        var action = pending; pending = null;
        var mc = Minecraft.getInstance();
        current = new Activation(action, new PressLease(), mc.player, mc.level, tick);
        dispatching = true;
        try {
            ActionDispatcher.press(action.kind() == WheelActionRef.Kind.KEY ? HudBinding.Kind.MAPPING : HudBinding.Kind.ACTION,
                    action.target(), current.lease(), true);
        } finally {
            dispatching = false;
            if (cancelAfterDispatch || !gameplay() || !sameWorld()) cancel();
            else if (current != null && !current.lease().active()) current = null;
        }
    }
    public static void afterTick() {
        if (!gameplay() || !sameWorld() || current != null && tick > current.started()) cancel();
    }
    public static void opening(net.minecraft.client.gui.screens.Screen screen) {
        if (screen != null) cancel();
    }
    public static void logout() { cancel(); }
    public static void cancel() {
        pending = null;
        // A raw PRESS listener may open a screen; finish broadcasting that press before RELEASE.
        if (dispatching) { cancelAfterDispatch = true; return; }
        cancelAfterDispatch = false;
        var ended = current; current = null;
        if (ended != null) try { ended.lease().end(); }
        catch (RuntimeException | LinkageError error) { FingertipApi.report("wheel-release:" + ended.action().target(), error); }
    }
    private WheelActionPulse() {}
}

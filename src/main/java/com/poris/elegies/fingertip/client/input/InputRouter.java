package com.poris.elegies.fingertip.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.poris.elegies.fingertip.Fingertip;
import com.poris.elegies.fingertip.api.FingertipApi;
import com.poris.elegies.fingertip.client.ClientSession;
import com.poris.elegies.fingertip.client.hud.HudEditorScreen;
import com.poris.elegies.fingertip.client.hud.HudLayout;
import com.poris.elegies.fingertip.core.PressLease;
import com.poris.elegies.fingertip.mixin.client.KeyMappingAccess;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Fingertip.MOD_ID, value = Dist.CLIENT)
public final class InputRouter {
    private static final PressLease[] LEASES = new PressLease[Fingertip.SLOT_COUNT];
    private static final boolean[] PRESSED = new boolean[Fingertip.SLOT_COUNT];
    private static long escapeSince;
    private static boolean emergencyOff;
    static { for (int i = 0; i < Fingertip.SLOT_COUNT; i++) LEASES[i] = new PressLease(); }
    public static boolean enabled() { return !emergencyOff && HudLayout.INSTANCE.enabled && ClientSession.ready(); }

    /** Basic controls stay native; every other gameplay action must have a HUD owner. */
    public static KeyMapping claim(InputConstants.Key key) {
        if (RawKeyRelay.active()) return null;
        var mc = Minecraft.getInstance();
        if (!enabled() || mc.player == null || !mc.player.isAlive() || mc.player.isSpectator() || !mc.isWindowActive() || mc.screen != null
                || mc.getOverlay() != null || key.equals(InputConstants.UNKNOWN) || key.getValue() == GLFW.GLFW_KEY_ESCAPE) return null;
        if (NativeInputPolicy.basicKey(key) || NativeInputPolicy.systemKey(key) || HotbarControl.preservesKey(key)) return null;
        try {
            var candidates = KeyMappingAccess.fingertip$lookup().getAll(key);
            for (KeyMapping mapping : candidates) {
                int id = ShortcutKeys.index(mapping);
                if (id < 0 || !HudLayout.INSTANCE.visible(id) || !ShortcutKeyRules.validOwner(id)) continue;
                return mapping;
            }
        } catch (RuntimeException error) { FingertipApi.report("input-router", error); }
        return null;
    }
    public static void click(KeyMapping mapping) {
        var access = (KeyMappingAccess) mapping;
        access.fingertip$clickCount(access.fingertip$clickCount() + 1);
    }
    public static void releaseShared() {
        NativeInputPolicy.clearUnownedState();
        for (KeyMapping mapping : KeyMappingAccess.fingertip$all().values()) {
            KeyMapping owner = claim(mapping.getKey());
            if (owner != null && owner != mapping) mapping.setDown(false);
        }
    }
    public static void releaseAll() {
        PhysicalInputRouter.releaseWhitelisted();
        for (int i = 0; i < LEASES.length; i++) {
            try { LEASES[i].end(); } catch (RuntimeException | LinkageError error) { FingertipApi.report("release:" + i, error); }
            PRESSED[i] = false;
            ShortcutKeys.SLOTS[i].setDown(false);
            ((KeyMappingAccess)ShortcutKeys.SLOTS[i]).fingertip$clickCount(0);
        }
        // A pending native click must not fire after switching back to HUD ownership.
        for (var key : Minecraft.getInstance().options.keyHotbarSlots) {
            key.setDown(false);
            ((KeyMappingAccess)key).fingertip$clickCount(0);
        }
    }
    @SubscribeEvent public static void interaction(InputEvent.InteractionKeyMappingTriggered event) {
        // Native pick supersedes the current hand choice: a reply still awaited must not cancel the pick event
        // raised inside Minecraft.pickBlock.
        if (event.isPickBlock() && !HotbarControl.blocksVanillaSelection()) return;
        if (ClientSession.inputBlocked()) { event.setCanceled(true); event.setSwingHand(false); }
    }
    private static Object lastScreen, lastPlayer, lastInput;
    private static boolean lastTakeover;
    @SubscribeEvent public static void tick(ClientTickEvent.Pre event) {
        var mc = Minecraft.getInstance();
        ShortcutConfiguration.initialize();
        FingertipApi.invalidate();   // skills learned or lost show up within one tick
        // Settings screens (ours, Controls, other mods') and joining a world are where keys change: re-check then.
        // The mode and settings-object checks also catch code that assigns those fields without the setters.
        var layout = HudLayout.INSTANCE;
        if (mc.screen != lastScreen || mc.player != lastPlayer || layout.input != lastInput || layout.takeOverHotbar != lastTakeover) {
            lastScreen = mc.screen; lastPlayer = mc.player; lastInput = layout.input; lastTakeover = layout.takeOverHotbar;
            ShortcutKeyRules.changed();
        }
        ShortcutKeyRules.repair();
        ClientSession.updateSettings(HotbarControl.blocksVanillaSelection(), layout.input.useRealSlot);
        ClientSession.resumeAfterTransfer();
        if (!NativeInputPolicy.active()) PhysicalInputRouter.releaseWhitelisted();
        NativeKeyRelay.discardInactive();
        NativeInputPolicy.clearUnownedState();
        if (mc.player != null && InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_ESCAPE)) {
            long now = System.nanoTime();
            if (escapeSince == 0) escapeSince = now;
            if (!emergencyOff && now - escapeSince >= 3_000_000_000L) {
                emergencyOff = true; ClientSession.nativeChoice();
                mc.player.displayClientMessage(Component.translatable("poris_elegies_fingertip_incantations.emergency_off"), true);
            }
        } else escapeSince = 0;
        while (ShortcutKeys.OPEN.consumeClick()) if (mc.player != null && mc.screen == null) mc.setScreen(new HudEditorScreen(null, -1));
        for (int i = 0; i < Fingertip.SLOT_COUNT; i++) {
            KeyMapping key = ShortcutKeys.SLOTS[i];
            boolean active = enabled() && mc.player != null && mc.player.isAlive() && !mc.player.isSpectator()
                    && mc.isWindowActive() && mc.screen == null && HudLayout.INSTANCE.visible(i) && ShortcutKeyRules.validOwner(i);
            boolean clicked = false;
            while (key.consumeClick()) clicked = true;
            // One activation per physical press, including quick press+release within the same tick.
            if (active && clicked && !PRESSED[i]) { ActionDispatcher.press(i, LEASES[i]); PRESSED[i] = true; }
            if (!active || !key.isDown()) {
                try { LEASES[i].end(); } catch (RuntimeException | LinkageError error) { FingertipApi.report("release:" + i, error); }
                PRESSED[i] = false;
                if (!active) key.setDown(false);
            }
        }
        NativeKeyRelay.refreshHeld();
    }
    private InputRouter() {}
}

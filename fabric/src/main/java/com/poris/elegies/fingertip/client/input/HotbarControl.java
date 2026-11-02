package com.poris.elegies.fingertip.client.input;

import com.poris.elegies.fingertip.client.input.KeyState;
import com.mojang.blaze3d.platform.InputConstants;
import com.poris.elegies.fingertip.client.ClientSession;
import com.poris.elegies.fingertip.client.hud.HudLayout;
import com.poris.elegies.fingertip.hand.HandAccess;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/** The original nine storage slots stay usable; this policy only owns their selection controls. */
public final class HotbarControl {
    public static boolean managesGameplay() {
        var player = Minecraft.getInstance().player;
        return InputRouter.enabled() && player != null && player.isAlive() && !player.isSpectator();
    }
    public static boolean blocksVanillaSelection() {
        return managesGameplay() && HudLayout.INSTANCE.takeOverHotbar;
    }
    public static boolean nativeSlot(int id) { return !HudLayout.INSTANCE.takeOverHotbar && id >= 0 && id < 9; }
    public static KeyMapping displayKey(int id) {
        return nativeSlot(id) ? Minecraft.getInstance().options.keyHotbarSlots[id] : ShortcutKeys.SLOTS[id];
    }
    /** A saved custom shortcut can be paused by this mode without losing its binding. */
    public static boolean shortcutPaused(int id) {
        return id >= 0 && id < ShortcutKeys.SLOTS.length && !nativeSlot(id)
                && (preservesKey(KeyState.key(ShortcutKeys.SLOTS[id])) || OptionalKeyPolicy.paused(KeyState.key(ShortcutKeys.SLOTS[id]))
                    || ShortcutKeyRules.conflict(id) != null);
    }
    /**
     * Uses the player's actual hotbar bindings, including remapped number keys. The creative save/load hotbar keys
     * (C/X) are not kept: Fingertip takes them in mixed mode too (2026-10-26).
     */
    public static boolean preserves(KeyMapping mapping) {
        if (HudLayout.INSTANCE.takeOverHotbar || OptionalKeyPolicy.claimed(KeyState.key(mapping)) || OptionalKeyPolicy.wheelKey(KeyState.key(mapping))) return false;
        var o = Minecraft.getInstance().options;
        if (mapping == o.keyPickItem) return true;
        for (var key : o.keyHotbarSlots) if (key == mapping) return true;
        return false;
    }
    public static boolean preservesKey(InputConstants.Key key) {
        if (HudLayout.INSTANCE.takeOverHotbar || key.equals(InputConstants.UNKNOWN) || OptionalKeyPolicy.claimed(key) || OptionalKeyPolicy.wheelKey(key)) return false;
        var o = Minecraft.getInstance().options;
        if (KeyState.key(o.keyPickItem).equals(key)) return true;
        for (var mapping : o.keyHotbarSlots) if (KeyState.key(mapping).equals(key)) return true;
        return false;
    }
    /**
     * Pick block is a vanilla hotbar action, so it works only while a vanilla hotbar slot is the current choice
     * (2026-10-27, user). Fingertip mode never has one; in Mixed mode an item held through a HUD slot, or a choice
     * still waiting for the server, turns it off. With the HUD off it is plain vanilla.
     */
    public static boolean allowsPickBlock() {
        var player = Minecraft.getInstance().player;
        if (!managesGameplay()) return true;
        return !blocksVanillaSelection() && !ClientSession.inputBlocked() && !HandAccess.of(player.getInventory()).extended();
    }
    /** Explains a refused pick block instead of ignoring the key. */
    public static void refusePickBlock() {
        var player = Minecraft.getInstance().player;
        if (player != null) player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                blocksVanillaSelection() ? "poris_elegies_fingertip_incantations.pick_fingertip_mode" : "poris_elegies_fingertip_incantations.pick_native_only"), true);
    }
    /** Mixed controls may still hold an extended item; refill must follow that actual hand. */
    public static boolean ownsMainHand() {
        var player = Minecraft.getInstance().player;
        return managesGameplay() && (blocksVanillaSelection()
                || player != null && HandAccess.of(player.getInventory()).extended());
    }
    public static void toggle() {
        HudLayout.INSTANCE.setHotbarTakeover(!HudLayout.INSTANCE.takeOverHotbar);
        InputRouter.releaseAll();
        ClientSession.updateControl(blocksVanillaSelection());
        HudLayout.INSTANCE.save();
    }
    /** Apply every toggle immediately; off/on within one tick must still send both ownership transitions. */
    public static void setEnabled(boolean enabled) {
        if (HudLayout.INSTANCE.enabled == enabled) return;
        HudLayout.INSTANCE.enabled = enabled;
        InputRouter.releaseAll();
        if (!enabled) ClientSession.nativeChoice();
        ClientSession.updateControl(blocksVanillaSelection());
        HudLayout.INSTANCE.save();
    }
    private HotbarControl() {}
}

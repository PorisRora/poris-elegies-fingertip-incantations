package com.poris.elegies.fingertip.client.input;

import com.poris.elegies.fingertip.api.FingertipApi;
import com.poris.elegies.fingertip.binding.HudBinding;
import com.poris.elegies.fingertip.client.ClientSession;
import com.poris.elegies.fingertip.core.PressLease;
import com.poris.elegies.fingertip.mixin.client.KeyMappingAccess;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Bridges one pressed HUD slot to an action. Item selection never calls Item#use. */
public final class ActionDispatcher {
    public static void press(int hud, PressLease lease) {
        var binding = ClientSession.binding(hud);
        if (binding.kind() == HudBinding.Kind.EMPTY || binding.isItem()) {
            try { ClientSession.toggleSelection(hud); }
            catch (RuntimeException error) { FingertipApi.report(binding.action(), error); unavailable(); }
            return;
        }
        press(binding.kind(), binding.action(), lease, false);
    }
    /** Shared function dispatch; a wheel pulse owns its own lease and never changes a HUD binding. */
    public static void press(HudBinding.Kind kind, String id, PressLease lease, boolean oneShot) {
        try {
            switch (kind) {
                case MAPPING -> {
                    KeyMapping target = KeyMappingAccess.fingertip$all().get(id);
                    if (target == null || target.getName().startsWith(ShortcutKeys.PREFIX)) { unavailable(); return; }
                    if (!target.getKeyConflictContext().isActive()) { unavailable(); return; }
                    if (oneShot) NativeKeyRelay.pressOnce(target, lease); else NativeKeyRelay.press(target, lease);
                }
                case ACTION -> {
                    var action = FingertipApi.find(id).orElse(null);
                    if (action == null) { unavailable(); return; }
                    // 照发不判: Fingertip is only the HUD (2026-10-29, user: always send it, the owning mod has the last
                    // word; "我们只是hud"). A skill press always goes to its owner, which decides and explains a refusal
                    // itself. The state only draws the slot; judging it here as well could swallow a press the server
                    // would take, such as one made as a cooldown ends while its sync is still on the way.
                    lease.begin(action::press, action::release);
                }
                default -> { }
            }
        } catch (RuntimeException | LinkageError error) { FingertipApi.report(id, error); unavailable(); }
    }
    private static void unavailable() {
        if (Minecraft.getInstance().player != null)
            Minecraft.getInstance().player.displayClientMessage(Component.translatable("poris_elegies_fingertip_incantations.unavailable"), true);
    }
    private ActionDispatcher() {}
}

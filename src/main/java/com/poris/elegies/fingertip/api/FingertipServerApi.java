package com.poris.elegies.fingertip.api;

import com.poris.elegies.fingertip.server.ActionRelay;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * The server half of a skill adapter, for mods that have no packet of their own to start a skill (2026-10-28). A
 * client action calls {@link FingertipApi#sendToServer} from press() or release(); Fingertip carries the action ID to
 * the server and passes it, on the server thread, to the handler registered for the mod ID before the ID's colon. The
 * handler decides everything: whether the player may use the skill now, its cost and its cooldown.
 * <p>
 * Register on both sides, during mod construction or common setup. Each installed mod ID takes one handler; a second
 * one, or one for a mod that is not installed, is refused (2026-10-28, user: actions are keyed by mod ID).
 */
public final class FingertipServerApi {
    @FunctionalInterface
    public interface ActionHandler { void handle(ServerPlayer player, ResourceLocation action, boolean pressed); }

    public static void registerHandler(String modId, ActionHandler handler) { ActionRelay.register(modId, handler); }
    private FingertipServerApi() {}
}

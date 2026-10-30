package com.poris.elegies.fingertip.server;

import com.poris.elegies.fingertip.api.FingertipApi;
import com.poris.elegies.fingertip.api.FingertipServerApi;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Routes relayed skill presses (FingertipNetwork.ActionInput) to the handler of the action ID's mod, on the server thread. */
public final class ActionRelay {
    private static final Map<String, FingertipServerApi.ActionHandler> HANDLERS = new ConcurrentHashMap<>();

    public static void register(String modId, FingertipServerApi.ActionHandler handler) {
        Objects.requireNonNull(handler);
        if (!ModList.get().isLoaded(Objects.requireNonNull(modId)))
            throw new IllegalArgumentException("Fingertip server handler for " + modId + ", which is not an installed mod");
        if (HANDLERS.putIfAbsent(modId, handler) != null)
            throw new IllegalArgumentException("Duplicate Fingertip server handler for " + modId);
    }
    /** An installed mod without a handler is reported once; anything else a client sends is dropped. */
    public static void dispatch(ServerPlayer player, ResourceLocation action, boolean pressed) {
        String modId = action.getNamespace();
        var handler = HANDLERS.get(modId);
        if (handler == null) {
            if (ModList.get().isLoaded(modId))
                FingertipApi.report("server:" + modId, new IllegalStateException("No server handler for " + action));
            return;
        }
        try { handler.handle(player, action, pressed); }
        catch (RuntimeException | LinkageError error) { FingertipApi.report("server:" + modId, error); }
    }
    private ActionRelay() {}
}

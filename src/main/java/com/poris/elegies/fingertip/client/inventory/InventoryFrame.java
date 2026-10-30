package com.poris.elegies.fingertip.client.inventory;

import com.poris.elegies.fingertip.Fingertip;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/**
 * One fill per frame, on first use; HUD and editor share the same frame's summary. Nothing is built in a frame that
 * does not ask, as with the HUD hidden or switched off (2026-10-28: it used to be rebuilt every frame regardless).
 */
@EventBusSubscriber(modid = Fingertip.MOD_ID, value = Dist.CLIENT)
public final class InventoryFrame {
    private static final InventoryIndex INDEX = new InventoryIndex();
    private static long frame, built = -1;
    public static InventoryIndex index() {
        if (built != frame) { built = frame; INDEX.rebuild(Minecraft.getInstance().player); }
        return INDEX;
    }
    @SubscribeEvent public static void beforeFrame(RenderFrameEvent.Pre event) { frame++; }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { INDEX.reset(); }
    private InventoryFrame() {}
}

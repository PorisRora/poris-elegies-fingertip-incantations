package com.poris.elegies.fingertip.client;
import com.poris.elegies.fingertip.client.hud.*;
import com.poris.elegies.fingertip.client.input.*;
import com.poris.elegies.fingertip.client.inventory.InventoryFrame;
import com.poris.elegies.fingertip.network.FingertipNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;

public final class FingertipClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ShortcutKeys.register();
        ShortcutGlyphs.register();
        ClientSession.setup();
        ClientPlayNetworking.registerGlobalReceiver(FingertipNetwork.Profile.TYPE, (packet, ctx) -> FingertipNetwork.profileReceiver.accept(packet));
        ClientPlayNetworking.registerGlobalReceiver(FingertipNetwork.BindingUpdate.TYPE, (packet, ctx) -> FingertipNetwork.bindingReceiver.accept(packet));
        ClientPlayNetworking.registerGlobalReceiver(FingertipNetwork.Hand.TYPE, (packet, ctx) -> FingertipNetwork.handReceiver.accept(packet));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ClientSession.login();
            if (!ClientPlayNetworking.canSend(FingertipNetwork.Request.TYPE))
                handler.getConnection().disconnect(net.minecraft.network.chat.Component.literal("This server needs Fingertip Incantations for Fabric 1.21.1."));
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            WheelActionPulse.logout(); InputRouter.releaseAll(); ClientSession.logout(); InventoryFrame.logout();
        });
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            WheelActionPulse.beforeTick(); InputRouter.tick(); WheelActionPulse.start();
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientSession.diagnoseHand(); SwitchedLayers.tick(); WheelActionPulse.afterTick();
        });
        HudRenderCallback.EVENT.register(HudRenderer::render);
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            EditorEntry.inventory(screen);
            ScreenEvents.beforeRender(screen).register((s, g, mx, my, dt) -> { EditorEntry.position(s); TextFit.beforeScreen(); });
            ScreenEvents.afterRender(screen).register((s, g, mx, my, dt) -> TextFit.afterScreen(g, mx, my));
            ScreenMouseEvents.allowMouseClick(screen).register(EditorEntry::click);
            ScreenMouseEvents.allowMouseRelease(screen).register(EditorEntry::release);
        });
    }
}

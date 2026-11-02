package com.poris.elegies.fingertip.mixin.client;
import com.poris.elegies.fingertip.client.ClientSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientPacketListener.class)
abstract class ClientRespawnMixin {
    @Unique private LocalPlayer fingertip$oldPlayer;
    @Inject(method="handleRespawn", at=@At("HEAD"))
    private void fingertip$remember(ClientboundRespawnPacket packet, CallbackInfo ci) {
        var mc = Minecraft.getInstance();
        // PacketUtils re-enters this method on the render thread; never read player state on Netty.
        if (mc.isSameThread()) fingertip$oldPlayer = mc.player;
    }
    @Inject(method="handleRespawn", at=@At("TAIL"))
    private void fingertip$respawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (fingertip$oldPlayer != null && player != fingertip$oldPlayer) ClientSession.respawn(fingertip$oldPlayer, player);
        fingertip$oldPlayer = null;
    }
}

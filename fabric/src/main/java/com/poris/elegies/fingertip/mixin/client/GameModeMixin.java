package com.poris.elegies.fingertip.mixin.client;

import com.poris.elegies.fingertip.client.ClientSession;
import com.poris.elegies.fingertip.hand.HandAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class GameModeMixin {
    @Inject(method = "ensureHasSentCarriedItem", at = @At("HEAD"), cancellable = true)
    private void fingertip$keepSource(CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player != null && (ClientSession.inputBlocked() || HandAccess.of(player.getInventory()).extended())) ci.cancel();
    }
    @Inject(method = {"startDestroyBlock", "continueDestroyBlock"}, at = @At("HEAD"), cancellable = true)
    private void fingertip$waitForSelection(CallbackInfoReturnable<Boolean> ci) {
        if (ClientSession.inputBlocked()) ci.setReturnValue(false);
    }
}

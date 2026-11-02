package com.poris.elegies.fingertip.mixin.client;

import com.poris.elegies.fingertip.client.ClientSession;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
abstract class LocalPlayerMixin {
    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void fingertip$waitForSelection(boolean all, CallbackInfoReturnable<Boolean> ci) {
        if (ClientSession.inputBlocked()) ci.setReturnValue(false);
    }
}

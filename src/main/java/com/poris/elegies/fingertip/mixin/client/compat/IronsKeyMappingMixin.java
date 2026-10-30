package com.poris.elegies.fingertip.mixin.client.compat;

import com.poris.elegies.fingertip.client.input.NativeInputPolicy;
import com.poris.elegies.fingertip.client.input.NativeKeyRelay;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Optional Iron's bridge for its spell wheel switch (2026-10-29, user: "iron关轮盘好像没用", "关着也能打开"). Iron's keys are
 * a KeyMapping subclass whose setDown override marks itself consumable after calling super, and Iron's opens its spell
 * wheel from that mark through consume(). Fingertip blocks KeyMapping.setDown while it owns a key, but the override's own
 * line still ran, for example when a screen closed with the key held and vanilla set every mapping from the physical
 * keys. While Fingertip owns the key, the override does nothing and consume() reports no press, unless Fingertip itself
 * is relaying a press to it (a HUD slot bound to that key).
 */
@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.player.ExtendedKeyMapping", remap = false)
abstract class IronsKeyMappingMixin {
    @Shadow private boolean canBeConsumed;

    @Inject(method = "setDown", at = @At("HEAD"), cancellable = true)
    private void fingertip$ownedHold(boolean down, CallbackInfo ci) {
        if (NativeInputPolicy.refusesDown((KeyMapping)(Object)this, down)) ci.cancel();
    }

    @Inject(method = "consume", at = @At("HEAD"), cancellable = true)
    private void fingertip$ownedConsume(CallbackInfoReturnable<Boolean> cir) {
        var mapping = (KeyMapping)(Object)this;
        if (NativeInputPolicy.blocks(mapping) && !NativeKeyRelay.isHeld(mapping) && !NativeKeyRelay.hasClicks(mapping)) {
            canBeConsumed = false;
            cir.setReturnValue(false);
        }
    }
}

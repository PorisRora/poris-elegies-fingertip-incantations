package com.poris.elegies.fingertip.mixin.client;

import com.poris.elegies.fingertip.client.input.NativeInputPolicy;
import com.poris.elegies.fingertip.client.input.NativeKeyRelay;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.ToggleKeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ToggleKeyMapping.class)
abstract class ToggleKeyMappingMixin {
    @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
    private void fingertip$held(CallbackInfoReturnable<Boolean> cir) {
        var mapping = (KeyMapping)(Object)this;
        if (NativeInputPolicy.blocks(mapping) && !NativeKeyRelay.ownsToggle(mapping)) cir.setReturnValue(false);
    }
    @Inject(method = "setDown", at = @At("HEAD"), cancellable = true)
    private void fingertip$press(boolean down, CallbackInfo ci) {
        if (NativeInputPolicy.refusesDown((KeyMapping)(Object)this, down)) ci.cancel();
    }
}

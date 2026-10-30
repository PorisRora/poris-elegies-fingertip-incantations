package com.poris.elegies.fingertip.mixin.client;

import com.poris.elegies.fingertip.client.input.NativeInputPolicy;
import net.neoforged.neoforge.client.ClientHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** World input reaches mod listeners only through the target-specific HUD relay. */
@Mixin(ClientHooks.class)
abstract class RawInputHooksMixin {
    @Inject(method = "onKeyInput", at = @At("HEAD"), cancellable = true)
    private static void fingertip$key(int key, int scan, int action, int modifiers, CallbackInfo ci) {
        if (NativeInputPolicy.suppressRaw()) ci.cancel();
    }
    @Inject(method = "onMouseButtonPre", at = @At("HEAD"), cancellable = true)
    private static void fingertip$mousePre(int button, int action, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (NativeInputPolicy.suppressRaw()) cir.setReturnValue(false);
    }
    @Inject(method = "onMouseButtonPost", at = @At("HEAD"), cancellable = true)
    private static void fingertip$mousePost(int button, int action, int modifiers, CallbackInfo ci) {
        if (NativeInputPolicy.suppressRaw()) ci.cancel();
    }
}

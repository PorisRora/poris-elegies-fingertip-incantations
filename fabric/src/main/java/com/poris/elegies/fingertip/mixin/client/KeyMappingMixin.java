package com.poris.elegies.fingertip.mixin.client;

import com.poris.elegies.fingertip.client.input.KeyState;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.InputConstants;
import com.poris.elegies.fingertip.client.input.InputRouter;
import com.poris.elegies.fingertip.client.input.RawKeyRelay;
import com.poris.elegies.fingertip.client.input.NativeKeyRelay;
import com.poris.elegies.fingertip.client.input.NativeInputPolicy;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = KeyMapping.class, priority = 1100)
abstract class KeyMappingMixin {
    @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
    private void fingertip$leasedHold(CallbackInfoReturnable<Boolean> cir) {
        var mapping = (KeyMapping)(Object)this;
        if (NativeKeyRelay.isHeld(mapping)) cir.setReturnValue(KeyState.active(mapping));
        else if (NativeInputPolicy.blocks(mapping) && !NativeKeyRelay.ownsToggle(mapping)) cir.setReturnValue(false);
    }
    @Inject(method = "setDown", at = @At("HEAD"), cancellable = true)
    private void fingertip$physicalHold(boolean down, CallbackInfo ci) {
        if (NativeInputPolicy.refusesDown((KeyMapping)(Object)this, down)) ci.cancel();
    }
    @Inject(method = "consumeClick", at = @At("HEAD"), cancellable = true)
    private void fingertip$ownedClick(CallbackInfoReturnable<Boolean> cir) {
        var mapping = (KeyMapping)(Object)this;
        if (!NativeInputPolicy.blocks(mapping) && !NativeKeyRelay.hasClicks(mapping)) return;
        var access = (KeyMappingAccess)this;
        boolean allowed = NativeKeyRelay.consumeClick(mapping) && access.fingertip$clickCount() > 0;
        access.fingertip$clickCount(allowed ? access.fingertip$clickCount() - 1 : 0);
        cir.setReturnValue(allowed);
    }
    @Inject(method = "matches", at = @At("HEAD"), cancellable = true)
    private void fingertip$matches(int key, int scan, CallbackInfoReturnable<Boolean> cir) {
        if (NativeInputPolicy.blocks((KeyMapping)(Object)this)) cir.setReturnValue(false);
    }
    @Inject(method = "matchesMouse", at = @At("HEAD"), cancellable = true)
    private void fingertip$matchesMouse(int button, CallbackInfoReturnable<Boolean> cir) {
        if (NativeInputPolicy.blocks((KeyMapping)(Object)this)) cir.setReturnValue(false);
    }
    @Inject(method = "click", at = @At("HEAD"), cancellable = true)
    private static void fingertip$click(InputConstants.Key key, CallbackInfo ci) {
        var owner = InputRouter.claim(key);
        if (owner != null) { InputRouter.click(owner); ci.cancel(); }
        else if (NativeInputPolicy.nativeState(key, true, true)) ci.cancel();
    }
    @Inject(method = "set", at = @At("HEAD"), cancellable = true)
    private static void fingertip$hold(InputConstants.Key key, boolean held, CallbackInfo ci) {
        if (!held) {
            if (NativeInputPolicy.active()) {
                for (var slot : com.poris.elegies.fingertip.client.input.ShortcutKeys.SLOTS)
                    if (KeyState.key(slot).equals(key)) slot.setDown(false);
            }
            if (NativeInputPolicy.nativeState(key, false, false)) ci.cancel();
            return;
        }
        var owner = InputRouter.claim(key);
        if (owner != null) { owner.setDown(true); ci.cancel(); }
        else if (NativeInputPolicy.nativeState(key, true, false)) ci.cancel();
    }
    @Inject(method = "setAll", at = @At("TAIL"))
    private static void fingertip$held(CallbackInfo ci) { InputRouter.releaseShared(); }
    /**
     * When a screen closes, vanilla sets every mapping from the physical keys. A key Fingertip owns is skipped rather than
     * cancelled inside setDown, so a mod's setDown override cannot mark itself pressed after its super call (Iron's spell
     * wheel opened with its switch off that way; 2026-10-29).
     */
    @WrapOperation(method = "setAll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;setDown(Z)V"))
    private static void fingertip$ownedOnScreenClose(KeyMapping mapping, boolean down, Operation<Void> original) {
        if (NativeInputPolicy.refusesDown(mapping, down)) return;
        original.call(mapping, down);
    }
    /** Every rebind, ours or the Controls screen's, ends here: HUD shortcut standings are worked out again. */
    @Inject(method = "resetMapping", at = @At("TAIL"))
    private static void fingertip$remapped(CallbackInfo ci) { com.poris.elegies.fingertip.client.input.ShortcutKeyRules.changed(); }
}

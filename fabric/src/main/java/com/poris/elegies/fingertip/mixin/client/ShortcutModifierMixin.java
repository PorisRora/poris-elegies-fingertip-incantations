package com.poris.elegies.fingertip.mixin.client;

import com.poris.elegies.fingertip.client.input.KeyModifier;
import com.poris.elegies.fingertip.client.input.RawKeyRelay;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A bound Alt+Y action can be triggered by HUD G, without changing or pressing physical Alt. */
@Mixin(Screen.class)
abstract class ShortcutModifierMixin {
    @Inject(method = "hasAltDown", at = @At("HEAD"), cancellable = true)
    private static void fingertip$alt(CallbackInfoReturnable<Boolean> cir) {
        if (RawKeyRelay.active()) cir.setReturnValue(RawKeyRelay.modifier(KeyModifier.ALT));
    }
    @Inject(method = "hasControlDown", at = @At("HEAD"), cancellable = true)
    private static void fingertip$control(CallbackInfoReturnable<Boolean> cir) {
        if (RawKeyRelay.active()) cir.setReturnValue(RawKeyRelay.modifier(KeyModifier.CONTROL));
    }
    @Inject(method = "hasShiftDown", at = @At("HEAD"), cancellable = true)
    private static void fingertip$shift(CallbackInfoReturnable<Boolean> cir) {
        if (RawKeyRelay.active()) cir.setReturnValue(RawKeyRelay.modifier(KeyModifier.SHIFT));
    }
}

package com.poris.elegies.fingertip.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.platform.InputConstants;
import com.poris.elegies.fingertip.client.input.NativeInputPolicy;
import com.poris.elegies.fingertip.client.input.PhysicalInputRouter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Route before the public mouse event bus, including listeners with HIGHEST priority. */
@Mixin(MouseHandler.class)
abstract class MouseInputMixin {
    @WrapMethod(method = "onPress")
    private void fingertip$scope(long window, int button, int action, int modifiers, Operation<Void> original) {
        boolean previous = NativeInputPolicy.beginPhysical();
        try { original.call(window, button, action, modifiers); }
        finally { NativeInputPolicy.endPhysical(previous); }
    }
    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void fingertip$route(long window, int button, int action, int modifiers, CallbackInfo ci) {
        if (window != Minecraft.getInstance().getWindow().getWindow()) return;
        if (com.poris.elegies.fingertip.client.hud.RadialMenuScreen.route(button, action)
                || PhysicalInputRouter.route(InputConstants.Type.MOUSE.getOrCreate(button), action)) ci.cancel();
    }
}

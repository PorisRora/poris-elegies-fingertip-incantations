package com.poris.elegies.fingertip.mixin.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.poris.elegies.fingertip.client.hud.HudEditorScreen;
import com.poris.elegies.fingertip.client.input.NativeInputPolicy;
import com.poris.elegies.fingertip.client.input.PhysicalInputRouter;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Own a claimed press through its matching release, even if that press opens a screen. */
@Mixin(KeyboardHandler.class)
abstract class KeyboardMixin {
    @WrapMethod(method = "keyPress")
    private void fingertip$scope(long window, int code, int scan, int action, int modifiers, Operation<Void> original) {
        boolean previous = NativeInputPolicy.beginPhysical();
        boolean nativeKey = NativeInputPolicy.beginPassthrough(InputConstants.getKey(code, scan));
        try { original.call(window, code, scan, action, modifiers); }
        finally { NativeInputPolicy.endPassthrough(nativeKey); NativeInputPolicy.endPhysical(previous); }
    }
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void fingertip$route(long window, int code, int scan, int action, int modifiers, CallbackInfo ci) {
        var mc = Minecraft.getInstance();
        if (window != mc.getWindow().getWindow()) return;
        // Vanilla takes a screenshot or toggles fullscreen before any screen sees the key, so the key dialog never
        // got F2 (2026-10-26, user: "F2录不了吗"). While it is recording, those two keys go to the dialog first.
        if (action == GLFW.GLFW_PRESS && mc.screen instanceof HudEditorScreen editor && editor.capturing()
                && (mc.options.keyScreenshot.matches(code, scan) || mc.options.keyFullscreen.matches(code, scan))) {
            editor.keyPressed(code, scan, modifiers); ci.cancel(); return;
        }
        if (PhysicalInputRouter.route(InputConstants.getKey(code, scan), action)) ci.cancel();
    }
}

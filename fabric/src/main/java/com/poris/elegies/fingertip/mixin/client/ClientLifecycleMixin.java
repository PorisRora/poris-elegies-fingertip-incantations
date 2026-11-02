package com.poris.elegies.fingertip.mixin.client;
import com.poris.elegies.fingertip.client.EditorEntry;
import com.poris.elegies.fingertip.client.input.WheelActionPulse;
import com.poris.elegies.fingertip.client.inventory.InventoryFrame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class)
abstract class ClientLifecycleMixin {
    @Inject(method="runTick", at=@At("HEAD"))
    private void fingertip$frame(boolean tick, CallbackInfo ci) { InventoryFrame.beforeFrame(); }
    @Inject(method="setScreen", at=@At("HEAD"))
    private void fingertip$screen(Screen screen, CallbackInfo ci) { EditorEntry.opening(); WheelActionPulse.opening(screen); }

    @Inject(method="startAttack", at=@At("HEAD"), cancellable=true)
    private void fingertip$waitAttack(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (com.poris.elegies.fingertip.client.ClientSession.inputBlocked()) cir.setReturnValue(false);
    }
    @Inject(method="startUseItem", at=@At("HEAD"), cancellable=true)
    private void fingertip$waitUse(CallbackInfo ci) {
        if (com.poris.elegies.fingertip.client.ClientSession.inputBlocked()) ci.cancel();
    }
}

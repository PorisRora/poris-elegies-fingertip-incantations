package com.poris.elegies.fingertip.mixin.client;

import com.poris.elegies.fingertip.client.ClientSession;
import com.poris.elegies.fingertip.client.input.HotbarControl;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
abstract class ScrollSelectionMixin {
    @Inject(method = "swapPaint", at = @At("HEAD"), cancellable = true)
    private void fingertip$ownScroll(double direction, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.getInventory() == (Object)this && HotbarControl.blocksVanillaSelection()) ci.cancel();
    }
    @Inject(method = "swapPaint", at = @At("TAIL"))
    private void fingertip$scroll(double direction, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.getInventory() == (Object)this) ClientSession.nativeChoice();
    }
}

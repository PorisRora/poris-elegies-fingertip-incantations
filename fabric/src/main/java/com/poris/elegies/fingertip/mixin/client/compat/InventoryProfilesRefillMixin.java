package com.poris.elegies.fingertip.mixin.client.compat;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.poris.elegies.fingertip.client.input.HotbarControl;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional IPN bridge: its original hotbar is storage while HUD owns the main hand. */
@Pseudo
@Mixin(targets = "org.anti_ad.mc.ipnext.event.autorefill.AutoRefillHandler$ItemSlotMonitor", remap = false)
abstract class InventoryProfilesRefillMixin {
    @Shadow private int currentSlotId;
    @Shadow private boolean shouldHandle;
    @Unique private boolean fingertip$wasHudOwned;

    @ModifyExpressionValue(method = "updateCurrent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/Slot;getItem()Lnet/minecraft/world/item/ItemStack;", remap = true))
    private ItemStack fingertip$observeActualMainHand(ItemStack original) {
        var player = Minecraft.getInstance().player;
        // IPN compares both monitors to recognise a deliberate F swap. Observing the old
        // hotbar slot here makes its offhand monitor mistake a swap back for consumption.
        if (player != null && HotbarControl.ownsMainHand() && currentSlotId >= 36 && currentSlotId < 45)
            return player.getMainHandItem();
        return original;
    }

    @Inject(method = "checkShouldHandle", at = @At("TAIL"))
    private void fingertip$ownMainHandRefill(CallbackInfo ci) {
        // InventoryMenu 36..44 are the nine hotbar storage slots; armor and offhand keep IPN behavior.
        // Let checkHandle take its ordinary unhandled branch so its remembered stack and timer reset,
        // rather than leaving a queued refill that could run after HUD ownership changes.
        if (currentSlotId >= 36 && currentSlotId < 45) {
            boolean owned = HotbarControl.ownsMainHand();
            // On return to native controls, forget the virtual hand before resuming refill.
            // An empty native hotbar slot is not consumption of the former HUD item.
            if (owned || fingertip$wasHudOwned) shouldHandle = false;
            fingertip$wasHudOwned = owned;
        }
    }
}

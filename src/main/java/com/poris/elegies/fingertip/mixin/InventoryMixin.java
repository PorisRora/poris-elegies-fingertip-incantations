package com.poris.elegies.fingertip.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.poris.elegies.fingertip.diagnostics.HandDiagnostics;
import com.poris.elegies.fingertip.hand.HandAccess;
import com.poris.elegies.fingertip.hand.HandSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
abstract class InventoryMixin implements HandAccess {
    @Unique private final HandSource fingertip$source = new HandSource();
    @Override public HandSource fingertip$hand() { return fingertip$source; }
    // The hand trace only (HandDiagnostics.ENABLED): with it off, every Inventory.setItem just reads that flag.
    @Inject(method = "setItem", at = @At("HEAD"))
    private void fingertip$traceWrite(int slot, ItemStack stack, CallbackInfo ci) {
        if (HandDiagnostics.ENABLED) HandDiagnostics.sourceWrite(((Inventory)(Object)this).player, slot, stack);
    }
    @Inject(method = "getSelected", at = @At("HEAD"), cancellable = true)
    private void fingertip$read(CallbackInfoReturnable<ItemStack> result) {
        if (fingertip$source.extended()) result.setReturnValue(fingertip$source.read((Inventory)(Object)this));
    }
    @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
    private void fingertip$tool(BlockState block, CallbackInfoReturnable<Float> result) {
        if (fingertip$source.extended()) result.setReturnValue(fingertip$source.read((Inventory)(Object)this).getDestroySpeed(block));
    }
    @Inject(method = "removeFromSelected", at = @At("HEAD"), cancellable = true)
    private void fingertip$drop(boolean all, CallbackInfoReturnable<ItemStack> result) {
        if (fingertip$source.extended()) {
            var inventory = (Inventory)(Object)this;
            // Dropping changes inventory availability, just like cursor pickup. The selected
            // binding survives; the shared reconciliation resolves a reserve or waits for return.
            result.setReturnValue(fingertip$source.remove(inventory, all));
        }
    }
    @Inject(method = "tick", at = @At("TAIL"))
    private void fingertip$tickExternal(CallbackInfo ci) { fingertip$source.tickExternal((Inventory)(Object)this); }
    @ModifyExpressionValue(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Inventory;selected:I"))
    private int fingertip$tickingSource(int original) {
        return fingertip$source.lost() ? -1 : fingertip$source.index((Inventory)(Object)this);
    }
}

package com.poris.elegies.fingertip.mixin.client;

import com.poris.elegies.fingertip.client.InventoryPrediction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
abstract class ContainerPredictionMixin {
    @Unique private InventoryPrediction.Snapshot fingertip$before;

    @Inject(method = "clicked", at = @At("HEAD"))
    private void fingertip$capture(int slot, int button, ClickType type, Player player, CallbackInfo ci) {
        fingertip$before = InventoryPrediction.before(player);
    }
    @Inject(method = "clicked", at = @At("TAIL"))
    private void fingertip$invalidate(int slot, int button, ClickType type, Player player, CallbackInfo ci) {
        var before = fingertip$before; fingertip$before = null;
        InventoryPrediction.after(player, before);
    }
}

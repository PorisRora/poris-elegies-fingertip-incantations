package com.poris.elegies.fingertip.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.poris.elegies.fingertip.hand.HandAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemInHandRenderer.class)
abstract class HandRendererMixin {
    @ModifyExpressionValue(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Inventory;selected:I"))
    private int fingertip$reequip(int original) {
        var inventory = Minecraft.getInstance().player.getInventory();
        return HandAccess.of(inventory).index(inventory);
    }
}

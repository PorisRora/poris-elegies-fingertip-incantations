package com.poris.elegies.fingertip.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.poris.elegies.fingertip.server.SupplyContinuation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
abstract class SupplyFinishMixin {
    @WrapMethod(method = "completeUsingItem")
    private void fingertip$finish(Operation<Void> original) {
        var entity = (LivingEntity)(Object)this;
        var player = entity instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        var use = player != null && player.isUsingItem() ? SupplyContinuation.begin(player, player.getUsedItemHand()) : null;
        original.call();
        if (player != null) SupplyContinuation.finish(player, use);
    }
}

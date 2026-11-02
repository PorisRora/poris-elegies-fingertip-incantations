package com.poris.elegies.fingertip.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.poris.elegies.fingertip.hand.HandAccess;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayer.class)
abstract class ServerPlayerMixin {
    @ModifyExpressionValue(method = "drop(Z)Z", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Inventory;selected:I"))
    private int fingertip$dropSync(int original) {
        var inventory = ((ServerPlayer)(Object)this).getInventory();
        return HandAccess.of(inventory).index(inventory);
    }
}

package com.poris.elegies.fingertip.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.poris.elegies.fingertip.server.BindingService;
import com.poris.elegies.fingertip.server.SupplyContinuation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerPlayerGameMode.class)
abstract class SupplyUseMixin {
    @WrapMethod(method = "useItem")
    private InteractionResult fingertip$use(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand,
                                            Operation<InteractionResult> original) {
        var use = SupplyContinuation.begin(player, hand);
        // "Use real slot" covers only vanilla's handling; continuation and its Hand reply run after it.
        var result = BindingService.withRealSelected(player, hand, () -> original.call(player, level, stack, hand));
        if (result.consumesAction()) SupplyContinuation.finish(player, use);
        return result;
    }
    @WrapMethod(method = "useItemOn")
    private InteractionResult fingertip$place(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand,
                                              BlockHitResult hit, Operation<InteractionResult> original) {
        var use = SupplyContinuation.begin(player, hand);
        var result = BindingService.withRealSelected(player, hand, () -> original.call(player, level, stack, hand, hit));
        if (result.consumesAction()) SupplyContinuation.finish(player, use);
        return result;
    }
}

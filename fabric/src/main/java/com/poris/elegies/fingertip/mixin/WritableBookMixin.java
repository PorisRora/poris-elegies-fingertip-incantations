package com.poris.elegies.fingertip.mixin;

import com.poris.elegies.fingertip.hand.BookEditSessions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WritableBookItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WritableBookItem.class)
abstract class WritableBookMixin {
    @Inject(method = "use", at = @At("HEAD"))
    private void fingertip$pin(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> ci) {
        if (player instanceof ServerPlayer serverPlayer) BookEditSessions.begin(serverPlayer, hand);
    }
}

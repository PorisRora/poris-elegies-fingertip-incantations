package com.poris.elegies.fingertip.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.poris.elegies.fingertip.hand.HandAccess;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BookEditScreen.class)
abstract class BookEditorMixin {
    @Unique private int fingertip$openedSource;
    @Inject(method = "<init>", at = @At("TAIL"))
    private void fingertip$capture(Player owner, ItemStack book, InteractionHand hand, CallbackInfo ci) {
        fingertip$openedSource = hand == InteractionHand.MAIN_HAND ? HandAccess.of(owner.getInventory()).index(owner.getInventory()) : 40;
    }
    @ModifyExpressionValue(method = "saveChanges", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Inventory;selected:I"))
    private int fingertip$saveSource(int original) { return fingertip$openedSource; }
}

package com.poris.elegies.fingertip.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.poris.elegies.fingertip.client.hud.DurabilityPercent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A worn item showing its durability percent leaves out the bar the percent replaces (2026-10-30, user: "原版的耐久条就不用了
 * 吧"). Vanilla draws the bar before it runs the item decorators, and NeoForge has no hook to skip it.
 */
@Mixin(GuiGraphics.class)
abstract class ItemBarMixin {
    @WrapOperation(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isBarVisible()Z"))
    private boolean fingertip$percentReplacesBar(ItemStack stack, Operation<Boolean> original) {
        return original.call(stack) && !DurabilityPercent.shows(stack);
    }
}

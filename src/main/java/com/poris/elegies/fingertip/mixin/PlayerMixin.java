package com.poris.elegies.fingertip.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.poris.elegies.fingertip.hand.HandAccess;
import com.poris.elegies.fingertip.hand.HandSource;
import net.minecraft.core.NonNullList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
abstract class PlayerMixin {
    @Inject(method = "setItemSlot", at = @At("HEAD"), cancellable = true)
    private void fingertip$guardResult(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
        var inventory = ((Player)(Object)this).getInventory();
        var hand = HandAccess.of(inventory);
        if (slot != EquipmentSlot.MAINHAND || hand.writable(inventory)) return;
        // Whatever Fingertip keeps out of the hand goes into the bag, or at the feet when the bag is full; it is never
        // lost (2026-10-29). An empty HUD hand has nowhere to
        // hold it: Sophisticated Backpacks puts a placed backpack into the hand and then removes the block, which
        // deleted the backpack and everything in it (2026-10-28). A lost source, whose
        // slot no longer holds the chosen stack, still refuses the write itself, which would overwrite what lies there
        // now: an auto-refill mod that fills that slot on NeoForge's use-finish event, before vanilla hands back the
        // empty bottle, kept the refill and lost the bottle until 0.1.2-beta.
        if (!stack.isEmpty() && inventory.player instanceof net.minecraft.server.level.ServerPlayer player)
            HandSource.stowOrDrop(player, stack);
        ci.cancel();
    }
    // Preserve vanilla verification and onEquipItem; only redirect its actual main-hand write.
    @WrapOperation(method = "setItemSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;set(ILjava/lang/Object;)Ljava/lang/Object;"))
    private Object fingertip$write(NonNullList<ItemStack> list, int index, Object value, Operation<Object> original) {
        var inventory = ((Player)(Object)this).getInventory();
        var hand = HandAccess.of(inventory);
        if (list == inventory.items && hand.extended()) {
            if (inventory.player instanceof net.minecraft.server.level.ServerPlayer player)
                com.poris.elegies.fingertip.server.BindingService.followReplacement(player, hand.address(), hand.read(inventory), (ItemStack)value);
            var previous = hand.replace(inventory, (ItemStack)value);
            if (inventory.player instanceof net.minecraft.server.level.ServerPlayer player)
                com.poris.elegies.fingertip.server.BindingService.handResult(player);
            return previous;
        }
        return original.call(list, index, value);
    }
    // Right-clicking an entity (capturing a maid, milking a cow) gets the same "use real slot" and continuation as
    // using an item.
    @WrapMethod(method = "interactOn")
    private InteractionResult fingertip$interact(Entity entity, InteractionHand hand, Operation<InteractionResult> original) {
        if (!((Object)this instanceof net.minecraft.server.level.ServerPlayer player)) return original.call(entity, hand);
        var use = com.poris.elegies.fingertip.server.SupplyContinuation.begin(player, hand);
        var result = com.poris.elegies.fingertip.server.BindingService.withRealSelected(player, hand, () -> original.call(entity, hand));
        if (result.consumesAction()) com.poris.elegies.fingertip.server.SupplyContinuation.finish(player, use);
        return result;
    }
}

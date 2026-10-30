package com.poris.elegies.fingertip.mixin;

import com.poris.elegies.fingertip.hand.HandAccess;
import com.poris.elegies.fingertip.server.BindingService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStand.class)
abstract class ArmorStandMixin {
    // A completed deposit ends the selection before another right-click can reuse its empty destination.
    @Inject(method = "interactAt", at = @At("RETURN"))
    private void fingertip$settleHand(Player player, net.minecraft.world.phys.Vec3 hit, InteractionHand usedHand,
                                    CallbackInfoReturnable<net.minecraft.world.InteractionResult> result) {
        if (usedHand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer serverPlayer)
            BindingService.finishHandChange(serverPlayer);
    }

    // The second stand write is the exchange branch, after vanilla's slot locks and creative-copy branch.
    // A missing HUD hand has no destination for setItemInHand. Receive into real storage before removing
    // the stand's item, without weakening the guard that rejects late use results at a replaced source.
    @Inject(method = "swapItem", at = @At(value = "INVOKE", ordinal = 1,
            target = "Lnet/minecraft/world/entity/decoration/ArmorStand;setItemSlot(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V"), cancellable = true)
    private void fingertip$receive(Player player, EquipmentSlot slot, ItemStack held, InteractionHand usedHand,
                                  CallbackInfoReturnable<Boolean> result) {
        var inventory = player.getInventory();
        var hand = HandAccess.of(inventory);
        if (usedHand != InteractionHand.MAIN_HAND || !held.isEmpty() || !hand.extended() || hand.writable(inventory)) return;
        var stand = (ArmorStand)(Object)this;
        var incoming = stand.getItemBySlot(slot);
        if (incoming.isEmpty()) return;

        int destination = -1, empty = -1;
        for (int i = 0; i < inventory.items.size(); i++) {
            var stored = inventory.items.get(i);
            if (stored.isEmpty()) {
                if (empty < 0 && incoming.getCount() <= inventory.getMaxStackSize(incoming)) empty = i;
            } else if (ItemStack.isSameItemSameComponents(stored, incoming)
                    && inventory.getMaxStackSize(stored) - stored.getCount() >= incoming.getCount()) {
                destination = i;
                break;
            }
        }
        if (destination < 0) destination = empty;
        if (destination < 0) {
            if (player instanceof ServerPlayer serverPlayer) BindingService.sendHand(serverPlayer, "poris_elegies_fingertip_incantations.inventory_full");
            result.setReturnValue(false);
            return;
        }

        var stored = inventory.items.get(destination);
        if (stored.isEmpty()) inventory.setItem(destination, incoming);
        else stored.grow(incoming.getCount());
        stand.setItemSlot(slot, ItemStack.EMPTY);
        inventory.setChanged();
        // Existing inventory synchronization updates availability; receiving stock never selects a HUD slot.
        result.setReturnValue(true);
    }
}

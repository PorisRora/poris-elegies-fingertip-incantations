package com.poris.elegies.fingertip.inventory;

import com.poris.elegies.fingertip.api.inventory.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Only this adapter knows the vanilla storage/armor/offhand/cursor slot layout. */
public final class VanillaInventorySource implements InventorySource {
    @Override public String id() { return SourceAddress.VANILLA; }
    @Override public Component name() { return Component.translatable("poris_elegies_fingertip_incantations.source_vanilla"); }
    @Override public int size(Player player) { return 42; }
    @Override public SourceAccess access(Player player, int slot) {
        return slot < 36 ? SourceAccess.STORAGE : slot == 40 ? SourceAccess.OFFHAND : SourceAccess.OBSERVED;
    }
    @Override public ItemStack get(Player player, int slot) {
        return slot == 41 ? player.containerMenu.getCarried() : player.getInventory().getItem(slot);
    }
    @Override public void set(Player player, int slot, ItemStack stack) {
        if (slot < 0 || slot >= 36) throw new IllegalArgumentException("Not player storage");
        player.getInventory().setItem(slot, stack);
    }
    @Override public void changed(Player player) { player.getInventory().setChanged(); }
    @Override public void synchronize(ServerPlayer player) {
        player.containerMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) player.inventoryMenu.broadcastChanges();
    }
}

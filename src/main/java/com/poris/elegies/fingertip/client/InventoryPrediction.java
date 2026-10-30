package com.poris.elegies.fingertip.client;

import com.poris.elegies.fingertip.api.inventory.*;
import com.poris.elegies.fingertip.hand.HandAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Source-independent edit barrier, also available to a provider's custom predicted inventory UI. */
public final class InventoryPrediction {
    public record Snapshot(SourceAddress source, ItemStack reference, boolean wasEmpty,
                           Map<SourceAddress, ItemStack> pendingInventory) {}
    private static Map<SourceAddress, ItemStack> copyStorage(Player player) {
        var result = new LinkedHashMap<SourceAddress, ItemStack>();
        for (var entry : InventorySources.entries(player)) if (entry.access() == SourceAccess.STORAGE)
            result.put(entry.address(), entry.stack().copy());
        return result;
    }
    public static Snapshot before(Player player) {
        if (player != Minecraft.getInstance().player || !ClientSession.ready()) return null;
        var inventory = player.getInventory(); var hand = HandAccess.of(inventory);
        boolean selectedItem = ClientSession.selectedHud() >= 0 && ClientSession.binding(ClientSession.selectedHud()).isItem();
        if (ClientSession.inputBlocked() || selectedItem && (hand.address() == null || hand.lost()))
            return new Snapshot(hand.address(), ItemStack.EMPTY, true, copyStorage(player));
        if (hand.address() == null || hand.lost()) return null;
        var stack = InventorySources.read(player, hand.address());
        return new Snapshot(hand.address(), stack, stack.isEmpty(), null);
    }
    public static void after(Player player, Snapshot before) {
        if (before == null) return;
        boolean changed;
        if (before.pendingInventory() != null) {
            var after = copyStorage(player);
            changed = !before.pendingInventory().keySet().equals(after.keySet()) || before.pendingInventory().entrySet().stream()
                    .anyMatch(entry -> !ItemStack.matches(entry.getValue(), after.getOrDefault(entry.getKey(), ItemStack.EMPTY)));
        } else {
            var after = InventorySources.read(player, before.source());
            // F may deliberately leave this source empty. Editing a different slot must not
            // invalidate that return destination; a nonempty stack becoming empty still does.
            changed = after != before.reference() || after.isEmpty() && !before.wasEmpty()
                    || !InventorySources.selectable(player, before.source());
        }
        if (changed) ClientSession.inventoryEdited();
    }
    private InventoryPrediction() {}
}

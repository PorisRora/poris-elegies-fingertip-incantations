package com.poris.elegies.fingertip.server;

import com.poris.elegies.fingertip.binding.HudBinding;
import com.poris.elegies.fingertip.api.inventory.*;
import java.util.Objects;
import com.poris.elegies.fingertip.hand.HandAccess;
import com.poris.elegies.fingertip.diagnostics.HandDiagnostics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Immediate continuation proven by an actual use; BindingService separately recovers a lost active source. */
public final class SupplyContinuation {
    public record Use(int sequence, int hud, SourceAddress source, HudBinding binding, ItemStack stack) {}

    public static Use begin(ServerPlayer player, InteractionHand usedHand) {
        var inventory = player.getInventory(); var hand = HandAccess.of(inventory);
        // Mixed mode continues too while a HUD slot holds the hand.
        if (usedHand != InteractionHand.MAIN_HAND || !BindingService.ownsHotbar(player) && !hand.extended()) return null;
        if (hand.address() == null || hand.lost() || !hand.writable(inventory)) return null;
        if (hand.read(inventory).getCount() == 1)
            HandDiagnostics.event(player, "use-last-item", () -> "");
        return BindingService.supplyUse(player, hand.address(), hand.read(inventory));
    }

    public static void finish(ServerPlayer player, Use use) {
        try { continueUsedSupply(player, use); }
        finally { BindingService.finishHandChange(player); }
    }

    private static void continueUsedSupply(ServerPlayer player, Use use) {
        if (use == null || !player.isAlive() || player.isSpectator()) return;
        var now = InventorySources.read(player, use.source());
        // Used up: the stack ran out, or the use left another item in its place, such as an empty bucket or bottle.
        // That container stays where it is or joins a stack of its kind, and the hand moves on (2026-10-27, user).
        // A stack that is still there, or a result of the same kind, is not used up.
        boolean emptied = use.stack().isEmpty() && now.isEmpty();
        boolean replaced = !now.isEmpty() && now != use.stack() && !use.binding().matches(now);
        if (!emptied && !replaced) return;
        HandDiagnostics.event(player, "use-exhausted", () -> "useSource=" + use.source() + " useSequence=" + use.sequence()
                + " useHud=" + use.hud() + " result=" + HandDiagnostics.stack(now));
        var inventory = player.getInventory(); var hand = HandAccess.of(inventory);
        // Moved stacks and explicit invalidations cannot qualify.
        if (!Objects.equals(hand.address(), use.source()) || hand.lost() || !hand.writable(inventory)) return;
        if (replaced) stackWithItsKind(player, use.source(), now);
        hand.notifyChanges(inventory);
        BindingService.continueSupply(player, use);
    }

    /**
     * A leftover that stacks joins the lowest-numbered stack of its kind with room, so poured buckets gather on one
     * stack instead of each keeping a slot (2026-10-27, user). What does not fit stays where it is. Player storage only.
     */
    private static void stackWithItsKind(ServerPlayer player, SourceAddress source, ItemStack leftover) {
        if (!source.vanilla() || !leftover.isStackable()) return;
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.items.size() && !leftover.isEmpty(); slot++) {
            var stored = inventory.items.get(slot);
            if (slot == source.slot() || stored.isEmpty() || !ItemStack.isSameItemSameComponents(stored, leftover)) continue;
            int moved = Math.min(leftover.getCount(), inventory.getMaxStackSize(stored) - stored.getCount());
            if (moved > 0) { stored.grow(moved); leftover.shrink(moved); }
        }
        if (leftover.isEmpty()) inventory.setItem(source.slot(), ItemStack.EMPTY);
        inventory.setChanged();
    }

    private SupplyContinuation() {}
}

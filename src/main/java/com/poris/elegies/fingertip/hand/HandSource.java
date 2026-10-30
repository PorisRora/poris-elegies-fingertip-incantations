package com.poris.elegies.fingertip.hand;

import com.poris.elegies.fingertip.api.inventory.*;
import com.poris.elegies.fingertip.diagnostics.HandDiagnostics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.Objects;

/** Selected storage is a pinned live reference, never a copy or an Inventory.selected replacement. */
public final class HandSource {
    private SourceAddress address;
    private boolean controlled, lost;
    private boolean acceptedEmptyResult;
    private ItemStack expected = ItemStack.EMPTY;
    private ItemStack notified = ItemStack.EMPTY;
    private int revision;
    private final HandDiagnostics diagnostics = new HandDiagnostics();

    public HandDiagnostics diagnostics() { return diagnostics; }
    public String diagnosticState(Inventory inventory) {
        var actual = InventorySources.read(inventory.player, currentAddress(inventory));
        return "controlled=" + controlled + " source=" + address + " lost=" + lost + " revision=" + revision
                + " expected=" + HandDiagnostics.stack(expected) + " actual=" + HandDiagnostics.stack(actual)
                + " sameRef=" + (actual == expected) + " writable=" + writable(inventory)
                + " acceptedEmpty=" + acceptedEmptyResult;
    }

    public SourceAddress address() { return address; }
    public SourceAddress currentAddress(Inventory inventory) { return controlled ? address : SourceAddress.vanilla(inventory.selected); }
    // Compatibility boundary: -1 native, -2 HUD empty, -3 external, 0..35 vanilla storage.
    public int source() { return !controlled ? -1 : address == null ? -2 : address.legacySlot(); }
    public boolean extended() { return controlled; }
    public boolean lost() { return lost; }
    public int revision() { return revision; }
    public int index(Inventory inventory) { return controlled ? source() : inventory.selected; }
    public boolean at(Inventory inventory, SourceAddress candidate) { return Objects.equals(currentAddress(inventory), candidate); }
    public boolean writable(Inventory inventory) {
        return !controlled || (!lost && InventorySources.selectable(inventory.player, address)
                && (inventory.player.level().isClientSide || InventorySources.read(inventory.player, address) == expected));
    }
    public ItemStack read(Inventory inventory) {
        return writable(inventory) ? InventorySources.read(inventory.player, currentAddress(inventory)) : ItemStack.EMPTY;
    }
    public void select(Inventory inventory, int slot) { select(inventory, SourceAddress.vanilla(slot)); }
    public void select(Inventory inventory, SourceAddress selected) {
        if (!InventorySources.selectable(inventory.player, selected)) throw new IllegalArgumentException("Unavailable storage source");
        address = selected; controlled = true;
        expected = InventorySources.read(inventory.player, address);
        notified = address.vanilla() ? ItemStack.EMPTY : expected.copy();
        lost = acceptedEmptyResult = false; revision++;
        HandDiagnostics.event(inventory.player, "select-source", () -> "");
    }
    /** Fingertip lets go: the main hand is the vanilla selected slot again. Nothing in the inventory moves. */
    public void releaseToVanilla() {
        if (controlled || lost) revision++;
        controlled = lost = acceptedEmptyResult = false; address = null; expected = notified = ItemStack.EMPTY;
    }
    /** Fingertip keeps the hand and holds it empty, in either hotbar mode. Nothing in the inventory moves. */
    public void holdEmpty() {
        controlled = lost = true; acceptedEmptyResult = false;
        address = null; expected = notified = ItemStack.EMPTY; revision++;
    }
    /** A choice ends: Fingertip keeps the hand empty (holdEmpty), or gives it back to vanilla (releaseToVanilla). */
    public void letGo(boolean keepEmpty) { if (keepEmpty) holdEmpty(); else releaseToVanilla(); }
    /** Into the bag, or at the feet when it is full: what Fingertip keeps out of the hand is never lost (2026-10-29). */
    public static void stowOrDrop(ServerPlayer player, ItemStack stack) {
        player.getInventory().add(stack);
        if (!stack.isEmpty()) player.drop(stack, false);
    }
    /** A genuine vanilla setter, including potion/bucket results and F, writes to the pinned source. */
    public ItemStack replace(Inventory inventory, ItemStack value) {
        if (!writable(inventory)) return ItemStack.EMPTY;
        HandDiagnostics.event(inventory.player, "mainhand-result", () -> "result=" + HandDiagnostics.stack(value));
        var previous = read(inventory);
        if (InventorySources.write(inventory.player, currentAddress(inventory), value)) acceptResult(inventory);
        else if (!value.isEmpty() && inventory.player instanceof ServerPlayer player) {
            // A source that will not take it (another mod's storage): the bag, or the feet, as for a refused hand.
            stowOrDrop(player, value);
        }
        return previous;
    }
    public ItemStack remove(Inventory inventory, boolean all) {
        var stack = read(inventory);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        var removed = stack.split(all ? stack.getCount() : 1);
        HandDiagnostics.event(inventory.player, "drop", () -> "all=" + all + " removed=" + HandDiagnostics.stack(removed));
        InventorySources.changed(inventory.player, currentAddress(inventory));
        return removed;
    }
    public void acceptResult(Inventory inventory) {
        if (address == null) return;
        expected = InventorySources.read(inventory.player, address);
        // F can intentionally leave a known empty source. Keep that exact destination writable
        // for the return swap; depletion or an unrelated replacement must still invalidate it.
        acceptedEmptyResult = expected.isEmpty();
        notified = address.vanilla() ? ItemStack.EMPTY : expected.copy();
        lost = false; revision++;
    }
    public boolean validate(Inventory inventory) {
        notifyChanges(inventory);
        if (address == null || lost || inventory.player.level().isClientSide) return false;
        if (writable(inventory) && (!expected.isEmpty() || acceptedEmptyResult)) return false;
        HandDiagnostics.event(inventory.player, "validate-failed", () -> "reason="
                + (!InventorySources.selectable(inventory.player, address) ? "SOURCE_UNAVAILABLE"
                : InventorySources.read(inventory.player, address) != expected ? "STACK_REPLACED" : "STACK_EMPTY"));
        return invalidate(inventory);
    }
    public boolean invalidate(Inventory inventory) {
        if (address == null || lost) return false;
        lost = true; acceptedEmptyResult = false; inventory.player.stopUsingItem(); revision++;
        HandDiagnostics.event(inventory.player, "invalidate", () -> "");
        return true;
    }
    public void receive(Inventory inventory, boolean owned, SourceAddress selected, boolean unavailable) {
        if (!owned) { releaseToVanilla(); return; }
        if (selected == null) { holdEmpty(); return; }
        // Retain an unavailable provider's address; never reinterpret its slot as vanilla storage.
        controlled = true; address = selected;
        acceptedEmptyResult = false;
        expected = InventorySources.read(inventory.player, address);
        lost = unavailable || !InventorySources.selectable(inventory.player, address); revision++;
    }
    /** Notify the owning provider only when a live external stack actually changed. */
    public void notifyChanges(Inventory inventory) {
        if (inventory.player.level().isClientSide || address == null || address.vanilla() || !writable(inventory)) return;
        var actual = InventorySources.read(inventory.player, address);
        if (!ItemStack.matches(notified, actual)) {
            InventorySources.changed(inventory.player, address); notified = actual.copy();
        }
    }
    public void tickExternal(Inventory inventory) {
        if (address != null && !address.vanilla() && !lost) {
            var stack = read(inventory);
            if (!stack.isEmpty()) stack.inventoryTick(inventory.player.level(), inventory.player, address.slot(), true);
        }
    }
}

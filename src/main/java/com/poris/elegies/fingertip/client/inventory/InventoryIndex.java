package com.poris.elegies.fingertip.client.inventory;

import com.poris.elegies.fingertip.Fingertip;
import com.poris.elegies.fingertip.api.inventory.InventorySources;
import com.poris.elegies.fingertip.api.inventory.SourceAccess;
import com.poris.elegies.fingertip.api.inventory.SourceAddress;
import com.poris.elegies.fingertip.binding.HudBinding;
import com.poris.elegies.fingertip.core.SourceResolver;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.UUID;

/** Read-only inventory summary for one rendered frame. Never used to authorize an inventory operation.
 *  Rows, source views and hash storage are reused; no prior frame's contents are compared or retained. */
public final class InventoryIndex implements InventorySources.Visitor {
    private static final Hash.Strategy<ItemStack> ITEMS = new Hash.Strategy<>() {
        @Override public int hashCode(ItemStack stack) { return ItemStack.hashItemAndComponents(stack); }
        @Override public boolean equals(ItemStack a, ItemStack b) {
            return a == b || a != null && b != null && ItemStack.isSameItemSameComponents(a, b);
        }
    };
    private static final Comparator<Source> SOURCE_ORDER = Comparator.comparing(Source::address);
    private static final Row MISSING = new Row();
    private final Object2ObjectOpenCustomHashMap<ItemStack, Row> supplies = new Object2ObjectOpenCustomHashMap<>(ITEMS);
    private final Object2ObjectOpenHashMap<UUID, Row> equipment = new Object2ObjectOpenHashMap<>();
    /** Single items by kind, whatever their data: a by-kind supply slot (HudBinding.byKind) counts all of them. */
    private final Object2ObjectOpenHashMap<Item, Row> kinds = new Object2ObjectOpenHashMap<>();
    private final ObjectArrayList<Row> rows = new ObjectArrayList<>();
    private final ObjectArrayList<Source> sources = new ObjectArrayList<>();
    private int rowCount, sourceCount;

    /** Live stack references are valid only until the next rebuild/clear. Callers must not mutate them. */
    public static final class Source {
        private SourceAddress address;
        private ItemStack stack = ItemStack.EMPTY;
        public SourceAddress address() { return address; }
        public ItemStack stack() { return stack; }
        private void clear() { address = null; stack = ItemStack.EMPTY; }
    }

    /** Aggregates every matching source, independently of which one the hand currently uses. */
    public static final class Row {
        private final ObjectArrayList<Source> storage = new ObjectArrayList<>();
        private long total, offhandTotal;
        private boolean offhand;

        /** The 指尖检索法 as the server applies it: a duplicated equipment identity is counted like any other stock. */
        public SourceResolver.Status status() {
            if (!storage.isEmpty()) return SourceResolver.Status.READY;
            return offhand ? SourceResolver.Status.OFFHAND : SourceResolver.Status.MISSING;
        }
        public int total() { return (int)Math.min(Integer.MAX_VALUE, total); }
        /** HUD ownership count. Off-hand stock is shown but never offered as an automatic hand source. */
        public int displayTotal() { return (int)Math.min(Integer.MAX_VALUE, total + offhandTotal); }
        public int sourceCount() { return storage.size(); }
        public Source sourceAt(int index) { return storage.get(index); }

        /** The caller supplies a hand hint only for the selected HUD entry. Counts remain the whole row's total. */
        public Source source(SourceAddress held) {
            if (status() != SourceResolver.Status.READY) return null;
            if (held != null) {
                int low = 0, high = storage.size() - 1;
                while (low <= high) {
                    int middle = (low + high) >>> 1;
                    Source candidate = storage.get(middle);
                    int order = candidate.address.compareTo(held);
                    if (order == 0) return candidate;
                    if (order < 0) low = middle + 1; else high = middle - 1;
                }
            }
            return storage.getFirst();
        }
        private void add(Source source, SourceAccess access, int count) {
            if (access == SourceAccess.STORAGE) { storage.add(source); total += count; }
            else if (access == SourceAccess.OFFHAND) { offhand = true; offhandTotal += count; }
        }
        private void clear() {
            storage.clear(); total = offhandTotal = 0; offhand = false;
        }
    }

    public void rebuild(Player player) {
        clear();
        if (player == null) return;
        // visit enumerates each provider/slot address exactly once, including observed and offhand positions.
        InventorySources.visit(player, this);
        for (int i = 0; i < rowCount; i++) rows.get(i).storage.unstableSort(SOURCE_ORDER);
    }

    /** Drop old keys without hashing them: their live components may have changed since the last frame. */
    public void clear() {
        supplies.clear(); equipment.clear(); kinds.clear();
        for (int i = 0; i < rowCount; i++) rows.get(i).clear();
        for (int i = 0; i < sourceCount; i++) sources.get(i).clear();
        rowCount = sourceCount = 0;
    }

    /** Release high-water capacities when leaving a world; a normal frame only clears and reuses them. */
    public void reset() {
        clear(); supplies.trim(); equipment.trim(); kinds.trim(); rows.clear(); sources.clear(); rows.trim(); sources.trim();
    }

    private Row row() {
        if (rowCount == rows.size()) rows.add(new Row());
        return rows.get(rowCount++);
    }

    @Override public void accept(SourceAddress address, SourceAccess access, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (sourceCount == sources.size()) sources.add(new Source());
        Source source = sources.get(sourceCount++); source.address = address; source.stack = stack;
        Row supply = supplies.get(stack);
        if (supply == null) { supply = row(); supplies.put(stack, supply); }
        supply.add(source, access, stack.getCount());
        if (stack.getMaxStackSize() == 1) {
            Row kind = kinds.get(stack.getItem());
            if (kind == null) { kind = row(); kinds.put(stack.getItem(), kind); }
            kind.add(source, access, stack.getCount());
        }
        UUID identity = stack.get(Fingertip.IDENTITY.get());
        if (identity != null) {
            Row item = equipment.get(identity);
            if (item == null) { item = row(); equipment.put(identity, item); }
            item.add(source, access, stack.getCount());
        }
    }

    public Row find(HudBinding binding) {
        Row row = switch (binding.kind()) {
            case SUPPLY -> binding.byKind() ? kinds.get(binding.template().getItem()) : supplies.get(binding.template());
            case EQUIPMENT -> binding.identity() == null ? null : equipment.get(binding.identity());
            default -> null;
        };
        return row == null ? MISSING : row;
    }
}

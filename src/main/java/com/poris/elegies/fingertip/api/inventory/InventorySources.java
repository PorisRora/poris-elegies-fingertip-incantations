package com.poris.elegies.fingertip.api.inventory;

import com.poris.elegies.fingertip.inventory.VanillaInventorySource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Shared inventory boundary for resolution, hand I/O, binding and prediction. No stored item copies. */
public final class InventorySources {
    private static volatile Map<String, InventorySource> providers = Map.of(SourceAddress.VANILLA, new VanillaInventorySource());
    private static final Map<String, SourceAddress[]> addresses = new java.util.concurrent.ConcurrentHashMap<>();
    @FunctionalInterface public interface Visitor {
        void accept(SourceAddress address, SourceAccess access, ItemStack stack);
    }
    @FunctionalInterface public interface SearchVisitor {
        /** Return false to stop visiting; sources are ordered by SourceAddress.compareTo. */
        boolean accept(SourceAddress address, SourceAccess access, ItemStack stack);
    }
    public record Entry(SourceAddress address, SourceAccess access, ItemStack stack) {}

    public static synchronized void register(InventorySource source) {
        Objects.requireNonNull(source);
        new SourceAddress(source.id(), 0); // Validate before accepting an integration.
        // Sort once at registration so supply searches can stop at the lowest available address.
        var next = new TreeMap<String, InventorySource>(Comparator
                .comparing((String id) -> !SourceAddress.VANILLA.equals(id)).thenComparing(Comparator.naturalOrder()));
        next.putAll(providers);
        if (next.putIfAbsent(source.id(), source) != null) throw new IllegalArgumentException("Duplicate source: " + source.id());
        providers = Collections.unmodifiableMap(new LinkedHashMap<>(next));
    }
    private static int size(InventorySource source, Player player) {
        int size = source.size(player);
        if (size < 0 || size > 65536) throw new IllegalStateException("Invalid source size: " + source.id());
        return size;
    }
    private static InventorySource provider(Player player, SourceAddress address) {
        if (address == null) return null;
        var source = providers.get(address.provider());
        return source != null && address.slot() < size(source, player) ? source : null;
    }
    public static boolean selectable(Player player, SourceAddress address) {
        var source = provider(player, address);
        return source != null && source.access(player, address.slot()) == SourceAccess.STORAGE;
    }
    public static ItemStack read(Player player, SourceAddress address) {
        var source = provider(player, address);
        return source == null ? ItemStack.EMPTY : source.get(player, address.slot());
    }
    public static boolean write(Player player, SourceAddress address, ItemStack value) {
        var source = provider(player, address);
        if (source == null || source.access(player, address.slot()) != SourceAccess.STORAGE) return false;
        source.set(player, address.slot(), value);
        source.changed(player);
        return true;
    }
    public static void changed(Player player, SourceAddress address) {
        var source = provider(player, address);
        if (source != null) source.changed(player);
    }
    public static void synchronize(ServerPlayer player) {
        providers.values().forEach(source -> source.synchronize(player));
    }
    /** Vanilla already broadcasts its containers every tick; only additional sources need this hook. */
    public static void synchronizeExternal(ServerPlayer player) {
        providers.forEach((id, source) -> { if (!id.equals(SourceAddress.VANILLA)) source.synchronize(player); });
    }
    /** Call after a server-owned container transaction or storage topology change, never from changed(). */
    public static void committedEdit(ServerPlayer player) {
        com.poris.elegies.fingertip.server.BindingService.sourcesEdited(player);
    }
    public static Component name(SourceAddress address) {
        var source = address == null ? null : providers.get(address.provider());
        return source == null ? Component.translatable("poris_elegies_fingertip_incantations.missing") : source.name();
    }
    private static SourceAddress[] addresses(InventorySource source, int count) {
        return addresses.compute(source.id(), (id, old) -> {
            if (old != null && old.length >= count) return old;
            var next = new SourceAddress[count];
            for (int slot = 0; slot < count; slot++) next[slot] = new SourceAddress(id, slot);
            return next;
        });
    }
    /** Visit every source in address order without allocating a complete inventory snapshot. */
    public static void visit(Player player, Visitor visitor) {
        for (var source : providers.values()) {
            int count = size(source, player);
            var cached = addresses(source, count);
            for (int slot = 0; slot < count; slot++) visitor.accept(cached[slot], source.access(player, slot), source.get(player, slot));
        }
    }
    /** Operation lookup can stop immediately; the ordinary visitor still fills the entire HUD table. */
    public static void visitWhile(Player player, SearchVisitor visitor) {
        for (var source : providers.values()) {
            int count = size(source, player);
            var cached = addresses(source, count);
            for (int slot = 0; slot < count; slot++)
                if (!visitor.accept(cached[slot], source.access(player, slot), source.get(player, slot))) return;
        }
    }
    /** One view per operation; never cache live stack references across frames or a container edit. */
    public static List<Entry> entries(Player player) {
        var result = new ArrayList<Entry>();
        visit(player, (address, access, stack) -> result.add(new Entry(address, access, stack)));
        result.sort(Comparator.comparing(Entry::address));
        return result;
    }
    private InventorySources() {}
}

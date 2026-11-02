package com.poris.elegies.fingertip.hand;

import com.poris.elegies.fingertip.api.inventory.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.*;

/** An external book uses a packet marker; the opened session supplies its real source address. */
public final class BookEditSessions {
    private record Edit(SourceAddress source, ItemStack book) {}
    private static final Map<ServerPlayer, Edit> OPEN = new WeakHashMap<>();
    public static void begin(ServerPlayer player, InteractionHand hand) {
        var source = hand == InteractionHand.MAIN_HAND ? HandAccess.of(player.getInventory()).currentAddress(player.getInventory()) : null;
        if (InventorySources.selectable(player, source)) OPEN.put(player, new Edit(source, InventorySources.read(player, source)));
        else OPEN.remove(player);
    }
    public static boolean canSave(ServerPlayer player, int wireSlot) {
        Edit edit = OPEN.get(player);
        return edit != null && wireSlot == edit.source.legacySlot() && InventorySources.selectable(player, edit.source)
                && InventorySources.read(player, edit.source) == edit.book && edit.book.is(Items.WRITABLE_BOOK);
    }
    public static Object token(ServerPlayer player) { return OPEN.get(player); }
    public static boolean sameSession(ServerPlayer player, Object token) { return token != null && OPEN.get(player) == token; }
    public static ItemStack read(ServerPlayer player, int wireSlot) {
        return canSave(player, wireSlot) ? OPEN.get(player).book : ItemStack.EMPTY;
    }
    public static void write(ServerPlayer player, int wireSlot, ItemStack value) {
        if (canSave(player, wireSlot)) InventorySources.write(player, OPEN.get(player).source, value);
    }
    public static void finish(ServerPlayer player, int wireSlot) {
        Edit edit = OPEN.remove(player);
        if (edit == null || edit.source.legacySlot() != wireSlot) return;
        InventorySources.changed(player, edit.source);
        var hand = HandAccess.of(player.getInventory());
        if (Objects.equals(hand.address(), edit.source) && !hand.lost()) hand.acceptResult(player.getInventory());
        InventorySources.synchronize(player);
    }
    private BookEditSessions() {}
}

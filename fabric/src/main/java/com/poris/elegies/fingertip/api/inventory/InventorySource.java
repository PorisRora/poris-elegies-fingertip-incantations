package com.poris.elegies.fingertip.api.inventory;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Register once on both sides. All calls run on the owning game thread.
 * A provider exposes player-owned storage, not a second view of another provider's stacks.
 * get() MUST return the live stack, not a copy; its reference stays stable until replacement.
 * Persist, synchronize, tick and dispose of the storage in its owning mod. The source must be
 * unavailable (size 0) until its client mirror is ready. Send storage updates before hand replies.
 * Fingertip ticks the selected external stack; the provider must skip that stack in its own tick.
 * Container edits should use the normal menu path or bracket prediction with InventoryPrediction.
 */
public interface InventorySource {
    String id();
    Component name();
    int size(Player player);
    default SourceAccess access(Player player, int slot) { return SourceAccess.STORAGE; }
    ItemStack get(Player player, int slot);
    void set(Player player, int slot, ItemStack stack);
    /** Includes in-place durability/count/component changes; keep this cheap. */
    void changed(Player player);
    /** Called before acknowledgements and at server tick end; only send actual changes. */
    void synchronize(ServerPlayer player);
}

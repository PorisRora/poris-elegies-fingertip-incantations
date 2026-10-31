package com.poris.elegies.fingertip.server;

import com.google.common.collect.MapMaker;
import com.poris.elegies.fingertip.Fingertip;
import com.poris.elegies.fingertip.api.inventory.*;
import com.poris.elegies.fingertip.binding.HudBinding;
import com.poris.elegies.fingertip.core.SourceResolver;
import com.poris.elegies.fingertip.diagnostics.HandDiagnostics;
import com.poris.elegies.fingertip.hand.HandAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;
import static com.poris.elegies.fingertip.network.FingertipNetwork.*;

/** All inventory choices, identities and persistent item bindings are derived from server-owned stacks. */
public final class BindingService {
    private static final String STORAGE = Fingertip.MOD_ID;
    /**
     * One session per player object, compared as that object (2026-10-29). A WeakHashMap compared players by entity id,
     * which a respawned player keeps: the new player found the old one's entry, and lost it once the old object was
     * collected, so the server forgot the takeover and the choice some seconds after a death. A portal keeps the object.
     */
    private static final Map<ServerPlayer, Session> SESSIONS = new MapMaker().weakKeys().makeMap();
    private static final class Session {
        final List<HudBinding> bindings = new ArrayList<>(Fingertip.SLOT_COUNT);
        int sequence, selectedHud = -1, sentRevision = -1;
        boolean ownsHotbar, followingItem;
        /** "Use real slot" (使用真实格) from the client's settings; on until the client says otherwise. */
        boolean useRealSlot = true;
        /** The real vanilla selection while one right-click runs with selected moved to the held slot; -1 outside. */
        int compatOriginal = -1;
        /** An F the server accepted, until the hand holds something again: only then does an empty hand keep the choice. */
        boolean swappedAway;
        /** After such an F, the spot the chosen item left: where F again or the slot's key puts it back if still free. */
        SourceAddress returnTo;
        /** Slots cleared this tick, sent at its end: one as a BindingUpdate, several as the whole table. */
        final BitSet cleared = new BitSet(Fingertip.SLOT_COUNT);
        /** An inventory packet this tick changed the slot the client held from: answer at the end of it in any case. */
        boolean answerEdit;
        Session(ServerPlayer player) {
            ListTag saved = player.getPersistentData().getCompound(STORAGE).getList("bindings", Tag.TAG_COMPOUND);
            for (int i = 0; i < Fingertip.SLOT_COUNT; i++) {
                bindings.add(i < saved.size() ? HudBinding.load(saved.getCompound(i), player.registryAccess())
                        : HudBinding.empty());
            }
        }
    }
    private static HudBinding bindStorage(ItemStack actual) {
        if (actual.isEmpty()) return HudBinding.empty();
        boolean equipment = HudBinding.equipment(actual);
        UUID id = null;
        if (equipment) {
            id = actual.get(Fingertip.IDENTITY.get());
            if (id == null) { id = UUID.randomUUID(); actual.set(Fingertip.IDENTITY.get(), id); }
        }
        var template = actual.copyWithCount(1);
        if (!equipment) template.remove(Fingertip.IDENTITY.get());   // a kind or supply is not one marked item
        return new HudBinding(equipment ? HudBinding.Kind.EQUIPMENT : HudBinding.Kind.SUPPLY, id, template, "");
    }
    private static Session session(ServerPlayer player) { return SESSIONS.computeIfAbsent(player, Session::new); }
    private static void save(ServerPlayer player, Session state) {
        var list = new ListTag();
        state.bindings.forEach(binding -> list.add(binding.save(player.registryAccess())));
        var tag = new CompoundTag(); tag.put("bindings", list);
        player.getPersistentData().put(STORAGE, tag);
    }
    private static void profile(ServerPlayer player) {
        if (player.connection == null) return;   // a GameTest player that never joined
        session(player).cleared.clear();
        PacketDistributor.sendToPlayer(player, new Profile(List.copyOf(session(player).bindings)));
    }
    /** Just one slot's binding, as vanilla sends one changed container slot rather than the container (protocol 2). */
    private static void sendBinding(ServerPlayer player, int slot) {
        if (player.connection == null) return;
        var state = session(player);
        state.cleared.clear(slot);
        PacketDistributor.sendToPlayer(player, new BindingUpdate(slot, state.bindings.get(slot)));
    }
    public static void sendHand(ServerPlayer player, String message) {
        if (player.connection == null) return;   // a GameTest player that never joined
        Session state = session(player);
        var inventory = player.getInventory(); var hand = HandAccess.of(inventory);
        state.sentRevision = hand.revision();
        // Use the same inventory protocol as container clicks, rather than embedding a competing
        // snapshot in a hand reply which may arrive after a local drag or creative slot swap.
        InventorySources.synchronize(player);
        HandDiagnostics.event(player, "hand-send", () -> "sequence=" + state.sequence + " hud=" + state.selectedHud
                + " following=" + state.followingItem);
        PacketDistributor.sendToPlayer(player, new Hand(state.sequence, hand.extended(), hand.address(), state.selectedHud,
                hand.lost(), message));
    }
    public static boolean useRealSlot(ServerPlayer player) { return session(player).useRealSlot; }
    /**
     * "Use real slot" (使用真实格; the user's rule of 2026-10-27, named on 10-28, formerly "hotbar compatibility").
     * A few mods find the held item through Inventory.selected instead of the main hand: Sophisticated Backpacks opens "main inventory slot selected", which is a hotbar slot while the HUD holds
     * slot 20, so its screen closed at once. While the server handles one right-click of an item held from storage
     * 0-35, selected names that real slot; it is put back straight after, so nothing outside the call sees it.
     */
    public static <T> T withRealSelected(ServerPlayer player, net.minecraft.world.InteractionHand hand, java.util.function.Supplier<T> call) {
        var state = session(player);
        var inventory = player.getInventory();
        var held = HandAccess.of(inventory);
        int slot = held.extended() && !held.lost() ? held.source() : -1;
        if (hand != net.minecraft.world.InteractionHand.MAIN_HAND || !state.useRealSlot || state.compatOriginal >= 0
                || slot < 0 || slot >= 36 || slot == inventory.selected) return call.get();
        state.compatOriginal = inventory.selected;
        inventory.selected = slot;
        try { return call.get(); }
        finally {
            // A mod that picked another hotbar slot during the use keeps its choice.
            if (inventory.selected == slot) inventory.selected = state.compatOriginal;
            state.compatOriginal = -1;
        }
    }
    /**
     * Equipment follows what replaces it in the hand.
     * Touhou Little Maid swaps a held soul charm for a new empty one; an armor stand trades the held chestplate for
     * its own. The HUD slot is rebound to the item that ends up in the hand, which keeps or receives its own
     * identity; the old item keeps its identity, so none is ever copied. Copying it duplicated the identity when the
     * old item went somewhere unscanned, such as the armor stand. The binding stays when the old item is still in the
     * player's bag or off hand (F moves it to the off hand first), and for supplies, empty and non-single results.
     * Every HUD slot bound to the old item follows it, not just the selected one (2026-10-28, user: with F3 and F4
     * both bound, both change); the others would otherwise keep naming an item that is gone and show it missing.
     * Right-click wearing the chosen armour piece is the armour stand's exchange on the body (2026-10-28, user: "和盔甲架
     * 一个逻辑就行"): vanilla empties the held stack before the piece worn before comes back, so the old item is found
     * worn instead of in previous, and the slot follows the piece now in the hand. Worn armour does not count as
     * still being the player's, as the stand's does not.
     * A result exactly like a fresh item (preview19.29: an empty soul charm) binds its kind instead of one item.
     */
    public static void followReplacement(ServerPlayer player, SourceAddress source, ItemStack previous, ItemStack next) {
        var state = session(player);
        if (state.selectedHud < 0 || source == null || next.isEmpty() || next == previous || !HudBinding.single(next)) return;
        var binding = state.bindings.get(state.selectedHud);
        if (binding.kind() != HudBinding.Kind.EQUIPMENT || binding.matches(next)) return;
        if (!binding.matches(previous) && !(previous.isEmpty() && worn(player, binding))) return;
        UUID id = binding.identity();
        boolean[] elsewhere = {false};
        InventorySources.visitWhile(player, (address, access, stack) -> {
            elsewhere[0] = access != SourceAccess.OBSERVED && !address.equals(source) && id.equals(stack.get(Fingertip.IDENTITY.get()));
            return !elsewhere[0];
        });
        if (elsewhere[0]) return;
        var follow = bindStorage(next);
        int moved = 0;
        for (int i = 0; i < state.bindings.size(); i++) {
            var other = state.bindings.get(i);
            if (other.kind() != HudBinding.Kind.EQUIPMENT || !id.equals(other.identity())) continue;
            // Each slot keeps its own template stack, as a binding made by hand would.
            state.bindings.set(i, i == state.selectedHud ? follow
                    : new HudBinding(follow.kind(), follow.identity(), follow.template().copy(), follow.action()));
            moved++;
        }
        // A real change of what the slots hold: the one case besides binding that saves and sends the table.
        save(player, state); profile(player);
        int slots = moved;
        HandDiagnostics.event(player, "binding-follows-result", () -> "hud=" + state.selectedHud + " slots=" + slots
                + " result=" + HandDiagnostics.stack(next));
    }
    private static boolean worn(ServerPlayer player, HudBinding binding) {
        for (var slot : net.minecraft.world.entity.EquipmentSlot.values())
            if (slot.getType() == net.minecraft.world.entity.EquipmentSlot.Type.HUMANOID_ARMOR && binding.matches(player.getItemBySlot(slot)))
                return true;
        return false;
    }
    /** Vanilla's own hotbar slot packet or pick block: the hand is the vanilla selected slot again. */
    public static void vanillaChoice(ServerPlayer player) { endChoice(player, session(player), false); }
    /**
     * The held-item choice ends: a Fingertip empty hand, or the vanilla selected slot again. An F that left the item in
     * the off hand is forgotten only where the caller says so, as before (exhaustion, put-away, a change of mode).
     * Rule (2026-10-31, user: "指尖格丢失物品或者出什么问题一律回空手", "回指尖空手"): whatever ends it through the slot or
     * its item leaves a Fingertip empty hand in both modes; only vanilla's own choices and a change of mode give the hand
     * back to vanilla.
     */
    private static void endChoice(ServerPlayer player, Session state, boolean emptyHand) {
        player.stopUsingItem();
        state.selectedHud = -1; state.followingItem = false;
        HandAccess.of(player.getInventory()).letGo(emptyHand);
    }
    public static boolean ownsHotbar(ServerPlayer player) {
        return session(player).ownsHotbar && player.isAlive() && !player.isSpectator();
    }
    /** Item following belongs to a selected HUD item, even when native hotbar selection is allowed. */
    private static boolean followsHudItem(ServerPlayer player) {
        return session(player).selectedHud >= 0 && player.isAlive() && !player.isSpectator()
                && HandAccess.of(player.getInventory()).extended();
    }
    /** Stop following the bound item: a deliberate use or swap result stays in the hand, and so does the binding. */
    public static void stopFollowing(ServerPlayer player) {
        var state = session(player);
        state.followingItem = false;
        HandDiagnostics.event(player, "following-released", () -> "hud=" + state.selectedHud + " sequence=" + state.sequence);
    }
    public static void handResult(ServerPlayer player) {
        var state = session(player);
        if (state.selectedHud >= 0) {
            if (!state.bindings.get(state.selectedHud).matches(player.getMainHandItem())) stopFollowing(player);
            else state.followingItem = true;
        }
    }
    /**
     * A slot's key takes its item back from the off hand (2026-10-27, user), found only there by the normal lookup.
     * With the HUD holding another slot the two swap, as F does; with an empty hand the item goes into the first
     * empty storage slot. Null when there is no room, which leaves it in the off hand.
     */
    private static SourceAddress takeFromOffhand(ServerPlayer player, HudBinding binding) {
        var inventory = player.getInventory(); var hand = HandAccess.of(inventory);
        var item = player.getOffhandItem();
        if (!binding.matches(item)) return null;
        SourceAddress target = hand.extended() && hand.address() != null && hand.writable(inventory) ? hand.address() : null;
        if (target != null) {
            var held = InventorySources.read(player, target);
            if (!InventorySources.write(player, target, item)) return null;
            inventory.offhand.set(0, held);
        } else {
            var back = session(player).returnTo;
            if (back != null && InventorySources.selectable(player, back) && InventorySources.read(player, back).isEmpty()
                    && InventorySources.write(player, back, item)) {
                target = back;
            } else {
                int free = inventory.getFreeSlot();
                if (free < 0) return null;
                target = SourceAddress.vanilla(free);
                inventory.setItem(free, item);
            }
            inventory.offhand.set(0, ItemStack.EMPTY);
        }
        player.stopUsingItem();
        InventorySources.changed(player, target);
        HandDiagnostics.event(player, "offhand-taken-back", () -> "binding=" + HandDiagnostics.stack(item));
        return target;
    }
    static SupplyContinuation.Use supplyUse(ServerPlayer player, SourceAddress source, ItemStack stack) {
        Session state = session(player);
        if (state.selectedHud < 0) return null;
        var binding = state.bindings.get(state.selectedHud);
        // Unstackable supplies join too (2026-10-27, user): a water bucket or potion is used up like the last item of a stack.
        if (binding.kind() != HudBinding.Kind.SUPPLY || !binding.matches(stack)) return null;
        return new SupplyContinuation.Use(state.sequence, state.selectedHud, source, binding, stack);
    }
    static void continueSupply(ServerPlayer player, SupplyContinuation.Use use) {
        Session state = session(player);
        HandDiagnostics.event(player, "continue-supply", () -> "useSequence=" + use.sequence() + " sequence=" + state.sequence
                + " useHud=" + use.hud() + " hud=" + state.selectedHud + " sameBinding=" + (state.bindings.get(use.hud()) == use.binding()));
        if (!followsHudItem(player) || state.sequence != use.sequence() || state.selectedHud != use.hud()
                || state.bindings.get(use.hud()) != use.binding()) return;
        reconcileSelectedItem(player, state);
        sendHand(player, "");
    }
    /** Exhaustion ends the choice, including in mixed mode; replenishment never re-equips it. */
    private static void cancelItemSelection(ServerPlayer player, Session state) {
        HandDiagnostics.event(player, "selection-ended", () -> "hud=" + state.selectedHud);
        state.swappedAway = false; state.returnTo = null;
        endChoice(player, state, true);
    }
    /**
     * Settle an empty setter result only after its operation, so supply continuation gets first refusal. F is not a
     * hand-over (2026-10-27, user): after an F the server accepted, while the chosen item sits in the off hand and the
     * spot it left is still empty, the choice stays, so F again or the slot's key swaps it straight back there. Only a
     * real F counts: handing it to an armour stand, with a matching stack already in the off hand, still ends the choice
     * (checking only the offhand would miss these changes).
     * Once that F is done the hand is a Fingertip empty hand that remembers the spot the item left (2026-10-28, user:
     * "指尖格按F就是指尖空手"; a vanilla slot in mixed mode keeps vanilla's F). Before, the hand still pointed at that
     * spot, so a pickup landing there showed in the hand and ended the choice, and F again was refused.
     */
    public static void finishHandChange(ServerPlayer player) {
        var state = session(player);
        if (!player.getMainHandItem().isEmpty()) { state.swappedAway = false; state.returnTo = null; }
        if (!followsHudItem(player) || state.followingItem || !player.getMainHandItem().isEmpty()) return;
        var inventory = player.getInventory(); var hand = HandAccess.of(inventory);
        if (state.swappedAway && state.bindings.get(state.selectedHud).matches(player.getOffhandItem())) {
            if (hand.address() != null && hand.writable(inventory)) { state.returnTo = hand.address(); hand.holdEmpty(); return; }
            if (hand.address() == null && state.returnTo != null) return;
        }
        cancelItemSelection(player, state);
    }
    /**
     * F again while the chosen item waits in the off hand after an F (2026-10-28, user): it goes back to the spot it
     * left, or the first free storage slot when that is taken, and into the hand; with no room it stays in the off hand.
     * False when this is not that case, so vanilla's F runs.
     */
    public static boolean swapBack(ServerPlayer player) {
        var state = session(player); var inventory = player.getInventory(); var hand = HandAccess.of(inventory);
        if (!followsHudItem(player) || !state.swappedAway || state.returnTo == null || hand.address() != null) return false;
        var target = takeFromOffhand(player, state.bindings.get(state.selectedHud));
        if (target == null) { sendHand(player, "poris_elegies_fingertip_incantations.offhand_full"); return true; }
        hand.select(inventory, target);
        state.followingItem = true; state.swappedAway = false; state.returnTo = null;
        sendHand(player, "");
        return true;
    }
    /** ServerInputMixin accepted an F: an empty hand it leaves keeps the choice (see finishHandChange). */
    public static void offhandSwap(ServerPlayer player) { session(player).swappedAway = true; }
    /** Keep the live source, otherwise continue from current storage once. No stock means deselect. */
    private static void reconcileSelectedItem(ServerPlayer player, Session state) {
        if (state.selectedHud < 0) return;
        var binding = state.bindings.get(state.selectedHud);
        if (!binding.isItem()) return;
        var inventory = player.getInventory();
        var hand = HandAccess.of(inventory);
        var result = binding.findSource(player, hand.extended() ? hand.address() : null);
        if (!result.ready()) {
            cancelItemSelection(player, state);
            return;
        }
        state.followingItem = true;
        if (!hand.at(inventory, result.address()) || hand.lost() || !hand.writable(inventory)) {
            HandDiagnostics.event(player, "resolve-source", () -> "hud=" + state.selectedHud + " result=" + result);
            player.stopUsingItem();
            hand.select(inventory, result.address());
        }
    }
    /**
     * Vanilla handled an inventory packet (a click in a screen, a creative slot, the recipe book). The hand is judged
     * once per tick, by the check at its start, after all of that tick's packets (2026-10-29, replacing the client's
     * RECONCILE request and its wait, which came once per tick too). Not after each click: between picking the held
     * stack up and putting it down it is on the cursor, and a check then would take another stack or end the choice;
     * vanilla's slot number does not care what the slot holds, Fingertip follows the item. Until that check a hand
     * whose slot changed reads as empty on both sides, so nothing can act with the wrong item. When the client's own
     * prediction changed the held slot it let go of its hand, so that tick is answered even if the server's hand stays.
     */
    public static void inventoryPacket(ServerPlayer player, boolean touchedHand) {
        if (touchedHand) session(player).answerEdit = true;
    }
    /**
     * A storage mod's server-side transaction is done (InventorySources.committedEdit): the edit is complete, so check
     * the hand at once, and always tell the client, which may have predicted it.
     */
    public static void sourcesEdited(ServerPlayer player) {
        var inventory = player.getInventory();
        var hand = HandAccess.of(inventory);
        hand.validate(inventory);
        var state = session(player);
        // Preserve a deliberate F result. A depleted or removed active source gets one storage lookup.
        if (followsHudItem(player) && state.followingItem) reconcileSelectedItem(player, state);
        finishHandChange(player);
        sendHand(player, "");
    }
    /** Whether any of these slots of the menu is the vanilla storage slot the hand holds from. */
    public static boolean touchesHand(ServerPlayer player, net.minecraft.world.inventory.AbstractContainerMenu menu,
                                      java.util.stream.IntStream slots) {
        var hand = HandAccess.of(player.getInventory());
        int source = hand.extended() && hand.address() != null && hand.address().vanilla() ? hand.address().slot() : -1;
        if (source < 0) return false;
        return slots.anyMatch(i -> i >= 0 && i < menu.slots.size() && menu.getSlot(i).container == player.getInventory()
                && menu.getSlot(i).getContainerSlot() == source);
    }
    public static void request(ServerPlayer player, Request request) {
        Session state = session(player);
        HandDiagnostics.event(player, "request", () -> "operation=" + request.operation() + " incomingSequence=" + request.sequence()
                + " sequence=" + state.sequence + " requestedHud=" + request.hud() + " hud=" + state.selectedHud);
        if (request.sequence() <= state.sequence || request.sequence() < 0) return;
        state.sequence = request.sequence();
        if (request.operation() == SYNC) { profile(player); sendHand(player, ""); return; }
        if (request.operation() == SETTINGS) { applySettings(player, state, request.source()); return; }
        int hud = request.hud();
        if (hud < 0 || hud >= Fingertip.SLOT_COUNT || !player.isAlive() || player.isSpectator()) { sendHand(player, "poris_elegies_fingertip_incantations.unavailable"); return; }
        String message = "";
        boolean changed = false;
        switch (request.operation()) {
            case BIND_ITEM -> { message = bindItem(player, state, hud, request); changed = message.isEmpty(); }
            case SELECT, TOGGLE_SELECT -> message = selectItem(player, state, hud, request.operation() == TOGGLE_SELECT);
            case UNBIND -> { state.bindings.set(hud, HudBinding.empty()); changed = true; }
            case SWAP -> changed = swapSlots(state, hud, request.source());
            case BIND_HOTBAR -> changed = bindHotbar(player, state, hud, request.source());
            case BIND_ACTION -> changed = bindAction(state, hud, request.action());
            default -> { }
        }
        if (changed) bindingChanged(player, state, request.operation(), hud, request.source());
        // A choice the client predicted, and the server agrees with, needs no answer, as vanilla does not answer a
        // hotbar slot change. The change counts as sent, or the end of the tick
        // would send it after all.
        if (message.isEmpty() && !changed && predictedHand(player, state, request)) {
            InventorySources.synchronize(player);
            state.sentRevision = HandAccess.of(player.getInventory()).revision();
            return;
        }
        sendHand(player, message);
    }
    /** SETTINGS: "use real slot", and the hotbar mode when the client says it changed. */
    private static void applySettings(ServerPlayer player, Session state, int flags) {
        state.useRealSlot = (flags & REAL_SLOT) != 0;
        // Only "use real slot" changed: nothing to do to the hand, nothing to answer.
        if ((flags & MODE_CHANGED) == 0) return;
        // The client let go of its choice for the new mode before sending; so does the server, and confirms.
        state.ownsHotbar = (flags & TAKEOVER) != 0 && player.isAlive() && !player.isSpectator();
        state.swappedAway = false; state.returnTo = null;
        endChoice(player, state, state.ownsHotbar);
        sendHand(player, "");
    }
    /** BIND_ITEM: the stack the client saw at that address. The reply's message, empty when it was bound. */
    private static String bindItem(ServerPlayer player, Session state, int hud, Request request) {
        SourceAddress source = request.address();
        if (!InventorySources.selectable(player, source)) return "poris_elegies_fingertip_incantations.inventory_changed";
        ItemStack actual = InventorySources.read(player, source);
        if (actual.isEmpty() || !ItemStack.isSameItemSameComponents(actual, request.expected()))
            return "poris_elegies_fingertip_incantations.inventory_changed";
        state.bindings.set(hud, bindStorage(actual));
        InventorySources.changed(player, source);
        return "";
    }
    /** SELECT or TOGGLE_SELECT: the slot's item into the hand, or put away. The reply's message, empty for none. */
    private static String selectItem(ServerPlayer player, Session state, int hud, boolean toggle) {
        var binding = state.bindings.get(hud);
        // Actions never replace a held-item choice, even if a client requests SELECT.
        if (binding.kind() == HudBinding.Kind.ACTION || binding.kind() == HudBinding.Kind.MAPPING) return "";
        var hand = HandAccess.of(player.getInventory());
        // Only a genuinely held item toggles off; an offhand-only binding must be taken back.
        if (toggle && state.selectedHud == hud && hand.extended() && binding.matches(player.getMainHandItem())) {
            // Putting it away leaves a Fingertip empty hand in either mode (2026-10-28, user: "指尖格其实选中再按可以
            // 指尖空手更合适 感觉比回原版格更合理"; 19.19 went back to the vanilla slot in mixed mode). A number key or
            // the scroll wheel takes a vanilla slot as before.
            cancelItemSelection(player, state);
            return "";
        }
        // The client asks only for an item it can see, and has already put it in its hand. When the server
        // finds nothing (its view is newer), the choice ends with an empty hand, as running out does, so a click
        // sent before the reply never uses the item that was put away (2026-10-28, user). A full bag that keeps
        // the item in the off hand leaves the hand as it was: that request waited for its reply.
        if (!binding.isItem()) { cancelItemSelection(player, state); return "poris_elegies_fingertip_incantations.unbound"; }
        var result = binding.findSource(player, hand.extended() && state.selectedHud == hud ? hand.address() : null);
        if (result.status() == SourceResolver.Status.OFFHAND) {
            var back = takeFromOffhand(player, binding);
            if (back == null) return "poris_elegies_fingertip_incantations.offhand_full";
            result = new SourceResolver.Result(SourceResolver.Status.READY, back);
        }
        if (!result.ready()) {
            String message = "poris_elegies_fingertip_incantations." + result.status().name().toLowerCase(Locale.ROOT);
            cancelItemSelection(player, state);
            return message;
        }
        SourceAddress source = result.address();
        // Internal SELECT remains idempotent; physical repeats use TOGGLE_SELECT above.
        if (!hand.at(player.getInventory(), source) || hand.lost() || !hand.writable(player.getInventory())) player.stopUsingItem();
        hand.select(player.getInventory(), source);
        state.selectedHud = hud;
        state.followingItem = true;
        state.swappedAway = false; state.returnTo = null;
        return "";
    }
    /** SWAP: two slots trade contents; their keys stay. An item in hand stays in hand, its choice moving with it. */
    private static boolean swapSlots(Session state, int hud, int other) {
        if (other < 0 || other >= Fingertip.SLOT_COUNT || other == hud) return false;
        var moved = state.bindings.get(hud);
        state.bindings.set(hud, state.bindings.get(other)); state.bindings.set(other, moved);
        if (state.selectedHud == hud) state.selectedHud = other;
        else if (state.selectedHud == other) state.selectedHud = hud;
        return true;
    }
    /** BIND_HOTBAR: what one of the nine hotbar slots holds now. */
    private static boolean bindHotbar(ServerPlayer player, Session state, int hud, int slot) {
        if (slot < 0 || slot >= 9) return false;
        state.bindings.set(hud, bindStorage(player.getInventory().getItem(slot)));
        return true;
    }
    /** BIND_ACTION: a mod action's ID, or "mapping:" and a key mapping's name. */
    private static boolean bindAction(Session state, int hud, String action) {
        if (action.isBlank() || action.length() > 256) return false;
        boolean mapping = action.startsWith("mapping:");
        state.bindings.set(hud, new HudBinding(mapping ? HudBinding.Kind.MAPPING : HudBinding.Kind.ACTION,
                null, ItemStack.EMPTY, mapping ? action.substring(8) : action));
        return true;
    }
    /** A request changed what a slot holds: save the table and send the change. */
    private static void bindingChanged(ServerPlayer player, Session state, int operation, int hud, int other) {
        // Rebinding/removing the selected HUD entry must not leave a hidden old hand intent. A swap moved the choice
        // along with its item. The hand stays a Fingertip empty hand in mixed mode too (see endChoice; mixed mode used
        // to go back to the vanilla slot here, the only slot-side end that did).
        if (operation != SWAP && state.selectedHud == hud) endChoice(player, state, true);
        save(player, state);
        // Binding sends that one slot before the reply, whose message names what the slot now holds. Clearing is
        // collected over the tick: removing several slots at once sends the table once (user: "批量操作发整份吧").
        if (operation == UNBIND) state.cleared.set(hud);
        else sendBinding(player, hud);
        if (operation == SWAP) sendBinding(player, other);
    }
    /** Whether the hand is now exactly what a predicted SELECT or TOGGLE_SELECT said the client already holds. */
    private static boolean predictedHand(ServerPlayer player, Session state, Request request) {
        if (request.operation() != SELECT && request.operation() != TOGGLE_SELECT || request.source() != PREDICTED) return false;
        var hand = HandAccess.of(player.getInventory());
        if (!hand.extended() || state.compatOriginal >= 0) return false;
        if (request.address() == null) return state.selectedHud < 0 && hand.address() == null;
        return state.selectedHud == request.hud() && !hand.lost() && request.address().equals(hand.address());
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { profile(player); sendHand(player, ""); }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) SESSIONS.remove(player);
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) {
        if (event.getOriginal().getPersistentData().contains(STORAGE))
            event.getEntity().getPersistentData().put(STORAGE, event.getOriginal().getPersistentData().getCompound(STORAGE).copy());
        // A new player object (a respawn, or leaving the End) carries on the client's mode, its real-slot setting and the
        // request numbering, so the client need not send them again (2026-10-29); the choice itself ends with the old one.
        if (event.getEntity() instanceof ServerPlayer player && event.getOriginal() instanceof ServerPlayer original
                && SESSIONS.get(original) instanceof Session old) {
            var state = session(player);
            state.ownsHotbar = old.ownsHotbar; state.useRealSlot = old.useRealSlot; state.sequence = old.sequence;
            if (state.ownsHotbar) HandAccess.of(player.getInventory()).holdEmpty();
        }
    }
    /**
     * A portal keeps this player and its session: tell the client what the hand holds at once, in the same batch as the
     * new dimension and inventory, as vanilla sends the selected hotbar slot. The client no longer asks after a portal or a respawn.

     */
    @SubscribeEvent public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sendHand(player, "");
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { profile(player); sendHand(player, ""); }
    }
    @SubscribeEvent public static void tickPre(PlayerTickEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var hand = HandAccess.of(player.getInventory());
            boolean unavailable = hand.address() != null && !InventorySources.selectable(player, hand.address());
            hand.validate(player.getInventory());
            var state = session(player);
            if (followsHudItem(player) && state.followingItem && (unavailable || hand.lost()))
                reconcileSelectedItem(player, state);
            finishHandChange(player);
        }
    }
    @SubscribeEvent public static void tickPost(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.connection != null) {
            finishHandChange(player);
            var hand = HandAccess.of(player.getInventory());
            // Vanilla use/mining can mutate the live stack without a setter.
            hand.notifyChanges(player.getInventory());
            var state = session(player);
            if (state.answerEdit || state.sentRevision != hand.revision()) { state.answerEdit = false; sendHand(player, ""); }
            else InventorySources.synchronizeExternal(player);
            if (!state.cleared.isEmpty()) {
                if (state.cleared.cardinality() == 1) sendBinding(player, state.cleared.nextSetBit(0));
                else profile(player);
            }
            if (HandDiagnostics.ENABLED && state.selectedHud >= 0) HandDiagnostics.observeEmpty(player, state.selectedHud,
                    state.bindings.get(state.selectedHud), followsHudItem(player) && state.followingItem,
                    () -> "sequence=" + state.sequence + " following=" + state.followingItem);
        }
    }
    private BindingService() {}
}

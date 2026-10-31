package com.poris.elegies.fingertip.client;

import com.poris.elegies.fingertip.Fingertip;
import com.poris.elegies.fingertip.api.inventory.*;
import com.poris.elegies.fingertip.binding.HudBinding;
import com.poris.elegies.fingertip.core.SourceResolver;
import com.poris.elegies.fingertip.hand.HandAccess;
import com.poris.elegies.fingertip.diagnostics.HandDiagnostics;
import com.poris.elegies.fingertip.network.FingertipNetwork;
import com.poris.elegies.fingertip.mixin.client.GameModeAccess;
import com.poris.elegies.fingertip.client.input.HotbarControl;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static com.poris.elegies.fingertip.network.FingertipNetwork.*;

/**
 * Server-owned bindings and hand acknowledgements; no UI layout or input routing lives here.
 * <p>
 * Predict, then verify (2026-10-27, user): a choice the client can work out itself with the server's own 指尖检索法 is
 * applied at once, and input carries on, since the server handles the request before any click sent after it; the reply
 * only confirms it, or corrects a rare stale view of the inventory. Vanilla hotbar changes never wait, as in vanilla. A
 * request whose outcome the client cannot know (taking an item back from the off hand, re-resolving after an inventory
 * edit, a change of mode) still holds attacking, using, mining and dropping until its reply.
 * <p>
 * As close to vanilla as it goes (2026-10-29, protocol 2): the server pushes the hand after a portal, sends one changed
 * binding instead of the table, stays silent when a predicted choice was right, checks the hand itself as it handles
 * vanilla's inventory packets, and takes a vanilla hotbar slot from vanilla's own packet. The client sends its mode and
 * "use real slot" as one settings message, on joining and on each change.
 */
@EventBusSubscriber(modid = Fingertip.MOD_ID, value = Dist.CLIENT)
public final class ClientSession {
    private static List<HudBinding> bindings = Collections.nCopies(Fingertip.SLOT_COUNT, HudBinding.empty());
    /** choiceSequence: the newest choice sent, older replies are stale; blockingSequence: the newest one input waits for. */
    private static int sequence, choiceSequence, blockingSequence, acknowledgedSequence, selectedHud = -1;
    private static int waitingHud = -1;
    private static boolean ready;
    /** The slot to pick up again after a portal, and how many ticks to wait for its item to sync; -1 when none. */
    private static int resumeHud = -1, resumeTicks;
    private static Boolean control, realSlot;
    public static boolean ready() { return ready; }
    public static boolean inputBlocked() { return blockingSequence > acknowledgedSequence; }
    public static int selectedHud() { return selectedHud; }
    public static int waitingHud() { return inputBlocked() ? waitingHud : -1; }
    public static HudBinding binding(int slot) { return bindings.get(slot); }
    /** The 指尖检索法 for one HUD slot; only the selected slot passes its current hand source, predicted or confirmed. */
    public static SourceResolver.Result findSource(int hud, Player player) {
        var hand = HandAccess.of(player.getInventory());
        return binding(hud).findSource(player, hand.extended() && selectedHud == hud ? hand.address() : null);
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        FingertipNetwork.profileReceiver = packet -> { bindings = packet.bindings(); ready = true; };
        FingertipNetwork.bindingReceiver = packet -> {
            if (packet.slot() < 0 || packet.slot() >= Fingertip.SLOT_COUNT) return;
            var changed = new ArrayList<>(bindings); changed.set(packet.slot(), packet.binding());
            bindings = List.copyOf(changed);
        };
        FingertipNetwork.handReceiver = ClientSession::receive;
    }
    private static void receive(Hand packet) {
        BindingFeedback.received(packet.sequence(), packet.message());
        var player = Minecraft.getInstance().player;
        if (player != null) HandDiagnostics.event(player, "hand-receive", () -> "incomingSequence=" + packet.sequence()
                + " choice=" + choiceSequence + " acknowledged=" + acknowledgedSequence
                + " incomingHud=" + packet.hud() + " incomingSource=" + packet.address() + " incomingLost=" + packet.lost());
        if (player == null || packet.sequence() < choiceSequence || packet.sequence() < acknowledgedSequence) return;
        acknowledgedSequence = packet.sequence();
        sequence = Math.max(sequence, packet.sequence());
        var inventory = player.getInventory();
        // The vanilla slot number stays vanilla's, as in vanilla: the client chooses it and vanilla's slot packets carry
        // it. Until 0.1.2-beta's review a reply set it too, and with no CLEAR left to make older replies stale, one
        // arriving after a vanilla choice set the number back while the server kept the new one; with no change of the
        // hand there, nothing corrected it.
        // Hand acknowledgements never write inventory. Vanilla menu packets own stack synchronization,
        // including creative inventory prediction and changes made while a selection is in flight.
        HandAccess.of(inventory).receive(inventory, packet.controlled(), packet.address(), packet.lost());
        selectedHud = packet.hud();
        waitingHud = -1;
        HandDiagnostics.event(player, "hand-applied", () -> "sequence=" + acknowledgedSequence + " hud=" + selectedHud);
        if (!packet.message().isBlank()) player.displayClientMessage(Component.translatable(packet.message()), true);
    }
    public static void request(int operation, int hud, int source, String action, ItemStack expected) {
        request(operation, hud, source, action, expected, operation == BIND_ITEM || operation == BIND_HOTBAR ? SourceAddress.legacy(source) : null);
    }
    public static void request(int operation, int hud, int source, String action, ItemStack expected, SourceAddress address) {
        request(operation, hud, source, action, expected, address,
                operation == UNBIND ? BindingFeedback.Operation.CLEAR : BindingFeedback.Operation.BIND);
    }
    private static void request(int operation, int hud, int source, String action, ItemStack expected, SourceAddress address,
                                BindingFeedback.Operation feedback) {
        // Only select() predicts a choice, and passes that on. Any other choice waits for its reply.
        request(operation, hud, source, action, expected, address, feedback, true);
    }
    private static void request(int operation, int hud, int source, String action, ItemStack expected, SourceAddress address,
                                BindingFeedback.Operation feedback, boolean blocking) {
        if (Minecraft.getInstance().player == null || (!ready && operation != SYNC)) return;
        HandDiagnostics.event(Minecraft.getInstance().player, "request-send", () -> "operation=" + operation
                + " nextSequence=" + (sequence + 1) + " hud=" + hud + " selectedHud=" + selectedHud + " blocked=" + inputBlocked()
                + " blocking=" + blocking);
        int next = ++sequence;
        if (operation == BIND_ITEM || operation == BIND_ACTION || operation == UNBIND || operation == BIND_HOTBAR || operation == SWAP)
            BindingFeedback.sent(next, hud, operation == SWAP ? source : -1, feedback);
        if (operation == SELECT || operation == TOGGLE_SELECT || operation == SETTINGS && blocking) {
            choiceSequence = next;
            if (blocking) {
                blockingSequence = next;
                waitingHud = operation == SETTINGS ? -1 : hud;
            }
            Minecraft.getInstance().player.stopUsingItem();
        }
        PacketDistributor.sendToServer(new Request(next, operation, hud, source, action, expected.copy(), address));
    }
    public static void select(int hud) {
        select(hud, false);
    }
    /** A physical press toggles the choice; internal reconciliation remains idempotent. */
    public static void toggleSelection(int hud) {
        select(hud, true);
    }
    private static void select(int hud, boolean toggle) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        if (HotbarControl.nativeSlot(hud)) {
            player.getInventory().selected = hud;
            nativeChoice();
            return;
        }
        HandDiagnostics.event(player, "hud-key", () -> "hud=" + hud + " selectedHud=" + selectedHud + " blocked=" + inputBlocked());
        var binding = binding(hud);
        if (!binding.isItem()) {
            if (binding.kind() == HudBinding.Kind.EMPTY)
                player.displayClientMessage(Component.translatable("poris_elegies_fingertip_incantations.unbound"), true);
            return;
        }
        var hand = HandAccess.of(player.getInventory());
        boolean putAway = toggle && !inputBlocked() && selectedHud == hud && hand.extended()
                && binding.matches(player.getMainHandItem());
        var resolution = findSource(hud, player);
        if (!putAway && !resolution.ready() && resolution.status() != SourceResolver.Status.OFFHAND) {
            player.displayClientMessage(Component.translatable("poris_elegies_fingertip_incantations."
                    + resolution.status().name().toLowerCase(java.util.Locale.ROOT)), true);
            return;
        }
        if (!putAway && resolution.status() == SourceResolver.Status.OFFHAND
                && !(hand.extended() && hand.address() != null && hand.writable(player.getInventory()))
                && player.getInventory().getFreeSlot() < 0) {
            player.displayClientMessage(Component.translatable("poris_elegies_fingertip_incantations.offhand_full"), true);
            return;
        }
        if (!toggle && !inputBlocked() && selectedHud == hud && resolution.ready()
                && hand.at(player.getInventory(), resolution.address()) && !hand.lost() && hand.writable(player.getInventory())) return;
        // Apply what the server is about to do. Only taking an item back from the off hand, or a press while another
        // reply is still awaited, has to wait.
        boolean blocking = inputBlocked() || !putAway && !resolution.ready();
        if (!blocking) {
            var inventory = player.getInventory();
            if (putAway) {
                // A Fingertip empty hand in either mode (2026-10-28, user), as the server does.
                selectedHud = -1;
                hand.holdEmpty();
            } else {
                if (!hand.at(inventory, resolution.address()) || hand.lost() || !hand.writable(inventory)) player.stopUsingItem();
                hand.select(inventory, resolution.address());
                selectedHud = hud;
            }
            HandDiagnostics.event(player, "hand-predicted", () -> "hud=" + selectedHud + " source=" + hand.address());
        }
        // A predicted choice carries the hand it put in place; the server answers only when it finds otherwise.
        request(toggle ? TOGGLE_SELECT : SELECT, hud, blocking ? -1 : PREDICTED, "", ItemStack.EMPTY,
                blocking || putAway ? null : resolution.address(), BindingFeedback.Operation.BIND, blocking);
    }
    public static void bindItem(int hud, int source) {
        bindItem(hud, SourceAddress.vanilla(source));
    }
    public static void bindItem(int hud, SourceAddress source) {
        if (nativeBindingLocked(hud)) return;
        var player = Minecraft.getInstance().player;
        if (player != null && InventorySources.selectable(player, source))
            request(BIND_ITEM, hud, -1, "", InventorySources.read(player, source), source);
    }
    public static void bindAction(int hud, String id) { if (!nativeBindingLocked(hud)) request(BIND_ACTION, hud, -1, id, ItemStack.EMPTY); }
    /** Two slots trade contents in one step; their keys stay. */
    public static void swap(int hud, int other) {
        if (hud == other || nativeBindingLocked(hud) || nativeBindingLocked(other)) return;
        request(SWAP, hud, other, "", ItemStack.EMPTY, null, BindingFeedback.Operation.SWAP);
    }
    public static void unbind(int hud) { unbind(hud, BindingFeedback.Operation.CLEAR); }
    public static void unbind(int hud, BindingFeedback.Operation feedback) {
        if (!nativeBindingLocked(hud)) request(UNBIND, hud, -1, "", ItemStack.EMPTY, null, feedback);
    }
    private static boolean nativeBindingLocked(int hud) {
        if (!HotbarControl.nativeSlot(hud)) return false;
        BindingFeedback.hint(hud, "poris_elegies_fingertip_incantations.native_slot_locked"); return true;
    }
    /**
     * A vanilla hotbar slot is chosen (number key, scroll wheel, pick block), or Fingertip lets go of the hand. Only
     * vanilla's own slot packet goes out, and the server ends a HUD choice when it arrives. While a HUD slot holds the hand, vanilla's number underneath stays where it was, so the

     * same number again would change nothing and vanilla would not send it: then it is sent here. With the HUD taking
     * over the hotbar the server ignores it; the settings message that follows a change of mode does the rest.
     */
    public static void nativeChoice() {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (!ready || player == null) return;
        var hand = HandAccess.of(player.getInventory());
        boolean heldByHud = selectedHud >= 0 || hand.extended();
        selectedHud = -1;
        hand.releaseToVanilla();
        if (!heldByHud || Boolean.TRUE.equals(control) || mc.getConnection() == null || mc.gameMode == null) return;
        int slot = player.getInventory().selected;
        mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
        ((GameModeAccess) mc.gameMode).fingertip$carriedIndex(slot);   // sent: vanilla's own check need not send it again
    }
    /**
     * After a dimension change, select the slot held before through the usual key path, once control is set up again
     * and its item has synced; gives up quietly after a second, as if nothing had been held.
     */
    public static void resumeAfterTransfer() {
        if (resumeHud < 0 || !ready || control == null || inputBlocked()) return;
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        int hud = resumeHud;
        if (!binding(hud).isItem()) { resumeHud = -1; return; }
        if (!findSource(hud, player).ready()) { if (--resumeTicks <= 0) resumeHud = -1; return; }
        resumeHud = -1;
        select(hud, false);
    }
    /**
     * The client's settings the server acts on: whether the HUD takes over the hotbar, and "use real slot". One message
     * on joining and on each change, as vanilla sends its client information (2026-10-29; two messages until 0.1.1-beta).
     * A change of mode lets go of the choice and waits for the server's answer; "use real slot" alone is not answered.
     * The server keeps both over a portal and a respawn.
     */
    public static void updateSettings(boolean takeover, boolean useRealSlot) {
        var player = Minecraft.getInstance().player;
        if (!ready || player == null) return;
        boolean modeChanged = !Boolean.valueOf(takeover).equals(control);
        if (!modeChanged && Boolean.valueOf(useRealSlot).equals(realSlot)) return;
        control = takeover; realSlot = useRealSlot;
        if (modeChanged) {
            selectedHud = -1;
            HandAccess.of(player.getInventory()).letGo(takeover);
        }
        int flags = (takeover ? TAKEOVER : 0) | (useRealSlot ? REAL_SLOT : 0) | (modeChanged ? MODE_CHANGED : 0);
        request(SETTINGS, -1, flags, "", ItemStack.EMPTY, null, BindingFeedback.Operation.BIND, modeChanged);
    }
    /** A change of mode only; "use real slot" as last sent (on until the client says otherwise, as on the server). */
    public static void updateControl(boolean enabled) { updateSettings(enabled, realSlot == null || realSlot); }
    /**
     * A menu click predicted here changed the held stack: let go of the hand until the server's answer, which it sends
     * as soon as it has handled the same click (2026-10-29). Nothing waits: the server checks the hand before any
     * action sent after the click, as vanilla's own state is right as soon as it handles one.
     */
    public static void inventoryEdited() {
        var player = Minecraft.getInstance().player;
        if (!ready || player == null) return;
        HandAccess.of(player.getInventory()).invalidate(player.getInventory());
    }
    @SubscribeEvent public static void diagnoseHand(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (!HandDiagnostics.ENABLED || mc.player == null || !ready || selectedHud < 0) return;
        HandDiagnostics.observeEmpty(mc.player, selectedHud, binding(selectedHud),
                mc.screen == null && mc.options.keyUse.isDown() && HotbarControl.ownsMainHand(),
                () -> "sequence=" + sequence + " choice=" + choiceSequence + " acknowledged=" + acknowledgedSequence
                        + " selectedHud=" + selectedHud);
    }
    /** The server sends the table and the hand as the player joins; nothing to ask (2026-10-29). */
    @SubscribeEvent public static void login(ClientPlayerNetworkEvent.LoggingIn event) { reset(); }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { reset(); }
    @SubscribeEvent public static void respawn(ClientPlayerNetworkEvent.Clone event) {
        // A portal is not a death: the slot held before is picked up again once the hand is set up in the new
        // dimension (2026-10-28, user agreed). Death still ends the choice.
        resumeHud = event.getOldPlayer().isDeadOrDying() ? -1 : selectedHud; resumeTicks = 20;
        // The server keeps this client's mode, real-slot setting and request numbering over a portal or a respawn, and
        // sends the hand right after, in the same batch, as vanilla sends the selected hotbar slot (2026-10-29, user:
        // "这样等于就是原版了 损耗也会变少"): nothing to ask or tell it. Until 0.1.1-beta the client asked again and re-sent
        // its mode, and dropped a kept choice for three ticks on the way.
        choiceSequence = blockingSequence = 0; selectedHud = waitingHud = -1;
        HandAccess.of(event.getNewPlayer().getInventory()).letGo(Boolean.TRUE.equals(control));
    }
    private static void reset() {
        BindingFeedback.reset();
        bindings = Collections.nCopies(Fingertip.SLOT_COUNT, HudBinding.empty());
        ready = false; sequence = choiceSequence = blockingSequence = acknowledgedSequence = 0; selectedHud = waitingHud = -1;
        control = realSlot = null; resumeHud = -1;
    }
    private ClientSession() {}
}

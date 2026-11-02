package com.poris.elegies.fingertip.mixin;

import com.poris.elegies.fingertip.hand.HandAccess;
import com.poris.elegies.fingertip.server.BindingService;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.stream.IntStream;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class ServerInputMixin {
    @Shadow public ServerPlayer player;
    // Vanilla's own slot packet ends a HUD choice in mixed mode (the client no longer sends CLEAR as well, 2026-10-29);
    // with the HUD taking over the hotbar it is ignored.
    @Inject(method = "handleSetCarriedItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V", shift = At.Shift.AFTER), cancellable = true)
    private void fingertip$vanilla(ServerboundSetCarriedItemPacket packet, CallbackInfo ci) {
        if (BindingService.ownsHotbar(player)) { ci.cancel(); return; }
        if (packet.getSlot() >= 0 && packet.getSlot() < 9) BindingService.vanillaChoice(player);
    }
    @Inject(method = "handlePickItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V", shift = At.Shift.AFTER))
    private void fingertip$pick(ServerboundPickItemPacket packet, CallbackInfo ci) { BindingService.vanillaChoice(player); }
    @Inject(method = "handlePlayerAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V", shift = At.Shift.AFTER), cancellable = true)
    private void fingertip$guardSwap(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        if (packet.getAction() != ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND) return;
        // F again while the chosen item waits in the off hand: Fingertip brings it back itself (2026-10-28).
        if (BindingService.swapBack(player)) { ci.cancel(); return; }
        if (!HandAccess.of(player.getInventory()).writable(player.getInventory())) {
            ci.cancel();
            BindingService.sendHand(player, HandAccess.of(player.getInventory()).address() == null
                    ? "poris_elegies_fingertip_incantations.offhand_select_first" : "poris_elegies_fingertip_incantations.inventory_changed");
        } else BindingService.offhandSwap(player);
    }
    // Inventory packets: the hand is judged once at the start of the next player tick, after all of this tick's
    // packets; the client no longer asks (RECONCILE, until 0.1.1-beta). A packet that changed the held slot gets an
    // answer in any case, as the client let go of its hand when its own prediction changed that slot.
    @Inject(method = "handleContainerClick", at = @At("RETURN"))
    private void fingertip$clicked(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        BindingService.inventoryPacket(player, BindingService.touchesHand(player, player.containerMenu,
                IntStream.concat(IntStream.of(packet.getSlotNum()), packet.getChangedSlots().keySet().intStream())));
    }
    @Inject(method = "handleSetCreativeModeSlot", at = @At("RETURN"))
    private void fingertip$creativeSlot(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci) {
        BindingService.inventoryPacket(player, BindingService.touchesHand(player, player.inventoryMenu, IntStream.of(packet.slotNum())));
    }
}

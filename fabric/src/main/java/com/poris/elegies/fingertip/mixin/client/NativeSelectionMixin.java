package com.poris.elegies.fingertip.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.poris.elegies.fingertip.client.ClientSession;
import com.poris.elegies.fingertip.client.input.HotbarControl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Inventory;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class NativeSelectionMixin {
    @Unique private boolean fingertip$serverPick;
    @WrapOperation(method = "handleKeybinds", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Inventory;selected:I", opcode = Opcodes.PUTFIELD))
    private void fingertip$nativeNumber(Inventory inventory, int value, Operation<Void> original) {
        if (HotbarControl.blocksVanillaSelection()) return;
        original.call(inventory, value); ClientSession.nativeChoice();
    }
    @Inject(method = "pickBlock", at = @At("HEAD"), cancellable = true)
    private void fingertip$pick(CallbackInfo ci) {
        fingertip$serverPick = false;
        // Pick block only while a vanilla hotbar slot is chosen, the wheel's included (2026-10-27, user).
        if (!HotbarControl.allowsPickBlock()) { HotbarControl.refusePickBlock(); ci.cancel(); }
        else ClientSession.nativeChoice();
    }
    @WrapOperation(method = "pickBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;handlePickItem(I)V"))
    private void fingertip$serverChoosesSlot(MultiPlayerGameMode mode, int slot, Operation<Void> original) {
        fingertip$serverPick = true;
        original.call(mode, slot);
    }
    @Inject(method = "pickBlock", at = @At("TAIL"))
    private void fingertip$picked(CallbackInfo ci) {
        // Survival pick from the backpack is completed by the server, which sends vanilla's slot packet itself.
        // Letting go here as well would send the client's OLD hotbar index over the server's new selection.
        if (!fingertip$serverPick) ClientSession.nativeChoice();
    }
}

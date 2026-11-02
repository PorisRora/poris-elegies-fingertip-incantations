package com.poris.elegies.fingertip.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.poris.elegies.fingertip.hand.BookEditSessions;
import com.poris.elegies.fingertip.api.inventory.SourceAddress;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class ServerBookMixin {
    @Shadow public ServerPlayer player;
    @Inject(method = "handleEditBook", at = @At("HEAD"))
    private void fingertip$mainThread(ServerboundEditBookPacket packet, CallbackInfo ci) {
        PacketUtils.ensureRunningOnSameThread(packet, (ServerGamePacketListenerImpl)(Object)this, player.serverLevel());
    }
    @ModifyExpressionValue(method = "handleEditBook", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;isHotbarSlot(I)Z"))
    private boolean fingertip$allowOpenedBook(boolean original, @Local(argsOnly = true) ServerboundEditBookPacket packet) {
        return original || BookEditSessions.canSave(player, packet.slot());
    }
    @WrapOperation(method = "handleEditBook", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;thenAcceptAsync(Ljava/util/function/Consumer;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<Void> fingertip$guardFilteredWrite(CompletableFuture<List<FilteredText>> future,
            Consumer<List<FilteredText>> write, Executor executor, Operation<CompletableFuture<Void>> original,
            @Local(argsOnly = true) ServerboundEditBookPacket packet) {
        int source = packet.slot();
        if ((source < 0 || source >= 36) && source != SourceAddress.EXTERNAL) return original.call(future, write, executor);
        var token = BookEditSessions.token(player);
        Consumer<List<FilteredText>> guarded = text -> {
            if (BookEditSessions.canSave(player, source) && BookEditSessions.sameSession(player, token)) {
                write.accept(text); BookEditSessions.finish(player, source);
            }
        };
        return original.call(future, guarded, executor);
    }
    @WrapOperation(method = {"updateBookContents", "signBook"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getItem(I)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack fingertip$readBook(Inventory inventory, int slot, Operation<ItemStack> original) {
        return slot == SourceAddress.EXTERNAL ? BookEditSessions.read(player, slot) : original.call(inventory, slot);
    }
    @WrapOperation(method = "signBook", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;setItem(ILnet/minecraft/world/item/ItemStack;)V"))
    private void fingertip$writeBook(Inventory inventory, int slot, ItemStack value, Operation<Void> original) {
        if (slot == SourceAddress.EXTERNAL) BookEditSessions.write(player, slot, value);
        else original.call(inventory, slot, value);
    }
}

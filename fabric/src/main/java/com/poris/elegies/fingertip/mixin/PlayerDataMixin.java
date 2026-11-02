package com.poris.elegies.fingertip.mixin;
import com.poris.elegies.fingertip.Fingertip;
import com.poris.elegies.fingertip.server.PlayerData;
import com.poris.elegies.fingertip.server.BindingService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
abstract class PlayerDataMixin implements PlayerData {
    @Unique private CompoundTag fingertip$data = new CompoundTag();
    public CompoundTag fingertip$data() { return fingertip$data; }
    @Inject(method="readAdditionalSaveData", at=@At("TAIL"))
    private void fingertip$read(CompoundTag tag, CallbackInfo ci) {
        fingertip$data = new CompoundTag();
        if (tag.contains(Fingertip.MOD_ID)) fingertip$data.put(Fingertip.MOD_ID, tag.getCompound(Fingertip.MOD_ID).copy());
        else if (tag.getCompound("NeoForgeData").contains(Fingertip.MOD_ID))
            fingertip$data.put(Fingertip.MOD_ID, tag.getCompound("NeoForgeData").getCompound(Fingertip.MOD_ID).copy());
    }
    @Inject(method="addAdditionalSaveData", at=@At("TAIL"))
    private void fingertip$write(CompoundTag tag, CallbackInfo ci) {
        if (fingertip$data.contains(Fingertip.MOD_ID)) tag.put(Fingertip.MOD_ID, fingertip$data.getCompound(Fingertip.MOD_ID).copy());
    }
    @Inject(method="tick", at=@At("HEAD"))
    private void fingertip$beforeTick(CallbackInfo ci) { BindingService.tickPre((ServerPlayer)(Object)this); }
    @Inject(method="tick", at=@At("TAIL"))
    private void fingertip$afterTick(CallbackInfo ci) { BindingService.tickPost((ServerPlayer)(Object)this); }
}

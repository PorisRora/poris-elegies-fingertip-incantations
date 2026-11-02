package com.poris.elegies.fingertip.mixin;

import com.poris.elegies.fingertip.Fingertip;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only the explicit creative clone operation strips identity; normal copy/transfer/render operations retain it. */
@Mixin(AbstractContainerMenu.class)
abstract class CreativeCloneMixin {
    @Inject(method = "doClick", at = @At("TAIL"))
    private void fingertip$newClone(int slot, int button, ClickType type, Player player, CallbackInfo ci) {
        if (type == ClickType.CLONE && player.getAbilities().instabuild)
            ((AbstractContainerMenu)(Object)this).getCarried().remove(Fingertip.IDENTITY.get());
    }
}

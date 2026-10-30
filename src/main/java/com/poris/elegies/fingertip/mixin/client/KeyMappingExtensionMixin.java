package com.poris.elegies.fingertip.mixin.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.poris.elegies.fingertip.client.input.RawKeyRelay;
import com.poris.elegies.fingertip.client.input.NativeInputPolicy;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.extensions.IKeyMappingExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IKeyMappingExtension.class)
interface KeyMappingExtensionMixin {
    @Inject(method = "isActiveAndMatches", at = @At("HEAD"), cancellable = true)
    private void fingertip$matches(InputConstants.Key key, CallbackInfoReturnable<Boolean> cir) {
        if (RawKeyRelay.virtualKey(key)) cir.setReturnValue(RawKeyRelay.targets(this)
                && ((IKeyMappingExtension)this).getKeyConflictContext().isActive());
        else if (NativeInputPolicy.blocks((KeyMapping)(Object)this)) cir.setReturnValue(false);
    }
}

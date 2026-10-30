package com.poris.elegies.fingertip.mixin.client;

import com.poris.elegies.fingertip.client.hud.HudRenderer;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
abstract class GuiMixin {
    @Inject(method = "renderItemHotbar", at = @At("HEAD"), cancellable = true)
    private void fingertip$hotbar(CallbackInfo ci) { if (HudRenderer.replacesHotbar()) ci.cancel(); }
}
